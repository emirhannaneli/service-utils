package net.lubble.util.spec

import co.elastic.clients.elasticsearch._types.SortOrder
import net.lubble.util.LK
import net.lubble.util.config.jackson.JacksonConfig
import net.lubble.util.config.utils.EnableLubbleUtilsConfig
import net.lubble.util.dto.RBase
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOptions
import net.lubble.util.model.SpecOptions
import net.lubble.util.spec.tool.SpecTool.SearchType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.Base64
import java.util.regex.Pattern
import co.elastic.clients.elasticsearch._types.query_dsl.Query as NativeQuery

class QueryToolsTest {
    class Owner {
        var name: String = ""

        @Field(type = FieldType.Keyword)
        var code: String = ""
    }

    class Line {
        var label: String = ""
    }

    class Product : BaseModel() {
        var title: String = ""

        @Field(type = FieldType.Keyword)
        var sku: String = ""

        @Field(name = "unit_price", type = FieldType.Double)
        var price: Double = 0.0
        var owner: Owner = Owner()

        @Field(type = FieldType.Nested)
        var lines: List<Line> = emptyList()
    }

    class ProductParam : ParameterModel() {
        var category: String? = null
    }

    class MongoSpec(param: ParameterModel = ParameterModel()) : BaseSpec.Mongo<Product>(param) {
        override var id: String? = null
        override var ids: List<String>? = null
        override var deleted: Boolean? = null
        override var archived: Boolean? = null
        override fun ofSearch(): Query = defaultQuery(param)
    }

    class ElasticSpec(param: ParameterModel = ParameterModel()) : BaseSpec.Elastic<Product>(param) {
        override var id: String? = null
        override var ids: List<String>? = null
        override var deleted: Boolean? = null
        override var archived: Boolean? = null
        override fun ofSearch(): Criteria = defaultCriteria()
    }

    class NativeSpec(param: ParameterModel = ParameterModel()) : BaseSpec.ElasticNative<Product>(param) {
        override var id: String? = null
        override var ids: List<String>? = null
        override var deleted: Boolean? = null
        override var archived: Boolean? = null
        override fun ofSearch(): NativeQuery = defaultQuery(param).build()._toQuery()
    }

    private fun param(configure: ProductParam.() -> Unit = {}) = ProductParam().apply(configure)

    // region Mongo

    @Test
    fun `mongo default query filters by id flags and nothing else`() {
        assertThat(MongoSpec().ofSearch().queryObject).isEmpty()

        val byPk = MongoSpec().apply { id = "42"; deleted = false; archived = true }.ofSearch().queryObject
        assertThat(byPk["pk"]).isEqualTo(42L)
        assertThat(byPk["deleted"]).isEqualTo(false)
        assertThat(byPk["archived"]).isEqualTo(true)

        assertThat(MongoSpec().apply { id = "abcde-12345-fghij" }.ofSearch().queryObject["sk"]).isEqualTo(LK("abcde-12345-fghij"))
    }

    @Test
    fun `mongo id helpers support nested fields and mixed id lists`() {
        val spec = MongoSpec()

        assertThat(spec.idQuery(Query(), "owner", "7").queryObject["owner.pk"]).isEqualTo(7L)
        assertThat(spec.idQuery(Query(), "owner", "abcde-12345-fghij").queryObject).containsKey("owner.sk")

        @Suppress("UNCHECKED_CAST")
        val mixed = spec.idsQuery(Query(), listOf("1", "2", "abcde-12345-fghij")).queryObject["\$or"] as List<Map<String, Any>>
        assertThat(mixed.flatMap { it.keys }).containsExactly("pk", "sk")
        assertThat(spec.idsQuery(Query(), "owner", listOf("1")).queryObject.toJson()).contains("owner.pk")
        assertThat(spec.idsQuery(Query(), emptyList()).queryObject).isEmpty()

        assertThat(spec.typeQuery(Product::class.java).queryObject["_class"]).isEqualTo(Product::class.java.name)
        assertThat(spec.typeQuery(Query(), Product::class.java).queryObject["_class"]).isEqualTo(Product::class.java.name)
    }

    private fun patterns(query: Query): List<Pattern> {
        val found = mutableListOf<Pattern>()
        fun walk(value: Any?) {
            when (value) {
                is Pattern -> found += value
                is Map<*, *> -> value.values.forEach(::walk)
                is Iterable<*> -> value.forEach(::walk)
            }
        }
        walk(query.queryObject)
        return found
    }

    @Test
    fun `mongo search builds one case insensitive pattern per word and field`() {
        val query = MongoSpec().searchQuery(Query(), "Ada  Lovelace", "name", "email")

        val patterns = patterns(query)
        assertThat(patterns).hasSize(4)
        assertThat(patterns.map { it.pattern() }.toSet()).containsExactlyInAnyOrder(Regex.escape("ada"), Regex.escape("lovelace"))
        assertThat(patterns).allMatch { it.flags() and Pattern.CASE_INSENSITIVE != 0 }
        assertThat(patterns[0].matcher("xx ADA yy").find()).isTrue()
    }

    @Test
    fun `mongo search treats regex metacharacters in user input literally`() {
        val evil = "(a+)+$"
        val pattern = patterns(MongoSpec().searchQuery(Query(), evil, "name")).single()

        assertThat(pattern.matcher("price is (a+)+$ today").find()).isTrue()
        assertThat(pattern.matcher("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!").find()).isFalse()
        assertThat(patterns(MongoSpec().searchQuery(Query(), ".*", "name")).single().matcher("anything").find()).isFalse()
    }

    @Test
    fun `mongo search types anchor the pattern and blank searches add nothing`() {
        val spec = MongoSpec()

        val starts = patterns(spec.searchQuery(Query(), "ada", "name", type = SearchType.STARTS_WITH)).single()
        assertThat(starts.matcher("Ada Lovelace").find()).isTrue()
        assertThat(starts.matcher("Countess Ada").find()).isFalse()

        val ends = patterns(spec.searchQuery(Query(), "ada", "name", type = SearchType.ENDS_WITH)).single()
        assertThat(ends.matcher("Countess Ada").find()).isTrue()
        assertThat(ends.matcher("Ada Lovelace").find()).isFalse()

        assertThat(spec.searchQuery(Query(), "ada", "name", type = SearchType.EQUAL).queryObject.toJson()).contains("\"ada\"")
        assertThat(spec.searchQuery(Query(), "   ", "name").queryObject).isEmpty()
    }

    // endregion

    // region Elastic (criteria)

    @Test
    fun `elastic default criteria and id helpers`() {
        val spec = ElasticSpec().apply { id = "42"; deleted = false }

        assertThat(spec.ofSearch().subCriteria).hasSize(2)
        assertThat(ElasticSpec().ofSearch().subCriteria).isEmpty()
        assertThat(spec.idCriteria("42").field!!.name).isEqualTo("pk")
        assertThat(spec.idCriteria("abcde-12345-fghij").field!!.name).isEqualTo("sk")
        assertThat(spec.idCriteria("owner", "42").field!!.name).isEqualTo("owner.pk")
        assertThat(spec.idCriteria(Criteria(), "42").subCriteria).hasSize(1)
        assertThat(spec.idCriteria(Criteria(), "owner", "42").subCriteria).hasSize(1)
        assertThat(spec.idsCriteria(Criteria(), listOf("1", "abcde-12345-fghij")).subCriteria).hasSize(1)
        assertThat(spec.idsCriteria(Criteria(), "owner", listOf("1")).subCriteria).hasSize(1)
        assertThat(spec.idsCriteria(Criteria(), emptyList()).subCriteria).isEmpty()
    }

    @Test
    fun `elastic sort resolves keyword sub fields and drops unknown fields`() {
        val spec = ElasticSpec(param { sortBy = "title,sku,price,owner.name,owner.code,unit_price,bogus,createdAt"; sortOrderString = "desc" })

        val orders = spec.buildQuery(spec.param, spec.ofSearch()).sort!!.toList()

        assertThat(orders.map { it.property })
            .containsExactly("title.keyword", "sku", "price", "owner.name.keyword", "owner.code", "unit_price", "createdAt")
        assertThat(orders).allMatch { it.direction == Sort.Direction.DESC }
    }

    @Test
    fun `elastic sort defaults to pk descending and honours the allow list`() {
        val unsorted = ElasticSpec()
        assertThat(unsorted.buildQuery(unsorted.param, unsorted.ofSearch()).sort).isEqualTo(Sort.by(Sort.Direction.DESC, "pk"))

        val restricted = ElasticSpec(param { sortBy = "title,price"; sortOrderString = "asc,desc" }).apply {
            options = SpecOptions().apply { sort = SortOptions().apply { allowedSortFields = hashSetOf("price") } }
        }
        assertThat(restricted.buildQuery(restricted.param, restricted.ofSearch()).sort).isEqualTo(Sort.by(Sort.Order.desc("price")))
    }

    // endregion

    // region Elastic (native)

    @Test
    fun `native default query filters by id and flags`() {
        val bool = NativeSpec().apply { id = "42"; ids = listOf("1", "abcde-12345-fghij"); deleted = false; archived = false }
            .ofSearch().bool()

        assertThat(bool.filter()).hasSize(4)
        assertThat(bool.filter()[0].term().field()).isEqualTo("pk")
        assertThat(bool.filter()[0].term().value().longValue()).isEqualTo(42)
        assertThat(bool.filter()[1].bool().should()).hasSize(2)
        assertThat(bool.filter()[2].term().field()).isEqualTo("deleted")
        assertThat(NativeSpec().ofSearch().bool().filter()).isEmpty()
    }

    @Test
    fun `native id helpers`() {
        val spec = NativeSpec()

        assertThat(spec.idQuery("abcde-12345-fghij").term().let { it.field() to it.value().stringValue() })
            .isEqualTo("sk" to "abcde-12345-fghij")
        assertThat(spec.idQuery("owner", "42").term().field()).isEqualTo("owner.pk")
        assertThat(spec.idQuery(spec.defaultQuery(spec.param), "42").build().filter()).hasSize(1)
        assertThat(spec.idQuery(spec.defaultQuery(spec.param), "owner", "42").build().filter()).hasSize(1)
        assertThat(spec.idsQuery(spec.defaultQuery(spec.param), listOf("1", "2")).build().filter().single().terms().field()).isEqualTo("pk")
        assertThat(spec.idsQuery(spec.defaultQuery(spec.param), "owner", listOf("abcde-12345-fghij")).build().filter().single().terms().field())
            .isEqualTo("owner.sk")
        assertThat(spec.idsQuery(spec.defaultQuery(spec.param), emptyList()).build().filter()).isEmpty()
    }

    @Test
    fun `native sort adds keyword suffixes nested paths and ignores unknown fields`() {
        val spec = NativeSpec(param { sortBy = "title,sku,price,owner.name,lines.label,bogus"; sortOrderString = "desc,asc" })

        val sorts = spec.nativeQueryBuilder(spec.param).build().sortOptions.map { it.field() }

        assertThat(sorts.map { it.field() }).containsExactly("title.keyword", "sku", "price", "owner.name.keyword", "lines.label")
        assertThat(sorts.map { it.order() }).containsExactly(SortOrder.Desc, SortOrder.Asc, SortOrder.Asc, SortOrder.Asc, SortOrder.Asc)
        assertThat(sorts.last().nested()!!.path()).isEqualTo("lines")
        assertThat(sorts.first().nested()).isNull()
    }

    @Test
    fun `native sort defaults to pk descending`() {
        val spec = NativeSpec()

        val sort = spec.nativeQueryBuilder(spec.param).build().sortOptions.single().field()

        assertThat(sort.field()).isEqualTo("pk")
        assertThat(sort.order()).isEqualTo(SortOrder.Desc)
    }

    // endregion

    // region specs

    @Test
    fun `specs resolve their model class`() {
        assertThat(MongoSpec().clazz).isEqualTo(Product::class.java)
        assertThat(ElasticSpec().clazz).isEqualTo(Product::class.java)
        assertThat(NativeSpec().clazz).isEqualTo(Product::class.java)
    }

    private fun decode(key: String) = String(Base64.getDecoder().decode(key))

    @Test
    fun `cache keys are stable and distinguish what the spec selects`() {
        val key = ElasticSpec().apply { id = "42" }.toCacheKey()

        assertThat(key).isEqualTo(ElasticSpec().apply { id = "42" }.toCacheKey())
        assertThat(key).isNotEqualTo(ElasticSpec().apply { id = "43" }.toCacheKey())
        assertThat(key).isNotEqualTo(ElasticSpec().apply { id = "42"; deleted = true }.toCacheKey())
        assertThat(key).isNotEqualTo(ElasticSpec().apply { id = "42"; fields = listOf("title") }.toCacheKey())
        assertThat(decode(key)).contains("\"id\":\"42\"")
        assertThat(MongoSpec().apply { id = "42" }.toCacheKey()).isEqualTo(MongoSpec().apply { id = "42" }.toCacheKey())
        assertThat(NativeSpec().apply { id = "42" }.toCacheKey()).isNotEqualTo(NativeSpec().toCacheKey())
    }

    @Test
    fun `cache keys include paging sorting and filter parameters`() {
        val base = ElasticSpec(param()).toCacheKey()

        assertThat(ElasticSpec(param { page = 2 }).toCacheKey()).isNotEqualTo(base)
        assertThat(ElasticSpec(param { size = 50 }).toCacheKey()).isNotEqualTo(base)
        assertThat(ElasticSpec(param { sortBy = "title" }).toCacheKey()).isNotEqualTo(base)
        assertThat(ElasticSpec(param { search = "ada" }).toCacheKey()).isNotEqualTo(base)
        assertThat(ElasticSpec(param { category = "books" }).toCacheKey()).isNotEqualTo(base)
        assertThat(decode(ElasticSpec(param { page = 2 }).toCacheKey())).contains("\"page\":2")
    }

    // endregion

    // region jackson

    class ProductRead : RBase() {
        var title: String? = "t"
        var amount: Int? = 1
    }

    @Test
    fun `read dtos are serialized with identifiers first and audit fields last`() {
        val mapper = JsonMapper.builder().addModule(JacksonConfig().customOrderModule()).build()
        val read = ProductRead().apply {
            pk = 1; sk = LK("abcde-12345-fghij"); createdAt = Instant.EPOCH; updatedAt = Instant.EPOCH; createdBy = "a"; updatedBy = "b"
        }

        val names = mapper.readTree(mapper.writeValueAsString(read)).propertyNames().toList()

        assertThat(names.take(2)).containsExactlyInAnyOrder("pk", "sk")
        assertThat(names.drop(2).take(2)).containsExactlyInAnyOrder("title", "amount")
        assertThat(names.takeLast(4)).containsExactlyInAnyOrder("updatedBy", "updatedAt", "createdBy", "createdAt")
    }

    @Test
    fun `library object mapper handles kotlin classes LK values and empty beans`() {
        data class Payload(val sk: LK, val name: String, val nested: Any = Any())

        val mapper = EnableLubbleUtilsConfig().mapper()
        val json = mapper.writeValueAsString(Payload(LK("abcde-12345-fghij"), "ada"))

        assertThat(json).contains("\"sk\":\"abcde-12345-fghij\"").contains("\"nested\":{}")
        assertThat(mapper.readValue("""{"sk":"abcde-12345-fghij","name":"ada"}""", Payload::class.java).sk).isEqualTo(LK("abcde-12345-fghij"))
    }

    // endregion
}
