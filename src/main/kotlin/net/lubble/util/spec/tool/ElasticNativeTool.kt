package net.lubble.util.spec.tool

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.SortOptions
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.spec.tool.SpecTool.IDType
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Field as ReflectField

interface ElasticNativeTool<T : BaseModel> {
    companion object {
        // Cache yapısını List olarak değiştirdik (tüm miras hiyerarşisi için)
        private val fieldCache = ConcurrentHashMap<Class<*>, List<ReflectField>>()
        private val columnNameCache = ConcurrentHashMap<ReflectField, String>()

        private fun normalizeString(str: String): String {
            return str.lowercase(Locale.ENGLISH)
        }
    }

    val clazz: Class<T>
    var id: String?
    var ids: List<String>?
    var deleted: Boolean?
    var archived: Boolean?

    fun ofSearch(): Query

    fun defaultQuery(param: ParameterModel): BoolQuery.Builder {
        val boolQuery = BoolQuery.Builder()

        id?.let {
            boolQuery.filter(createIdQueryInternal(null, it))
        }

        ids?.let {
            val idsQuery = createIdsQueryInternal(null, it)
            if (idsQuery != null) {
                boolQuery.filter(idsQuery)
            }
        }

        deleted?.let {
            boolQuery.filter { f ->
                f.term { t -> t.field("deleted").value(FieldValue.of(it)) }
            }
        }

        archived?.let {
            boolQuery.filter { f ->
                f.term { t -> t.field("archived").value(FieldValue.of(it)) }
            }
        }

        return boolQuery
    }

    fun nativeQueryBuilder(param: ParameterModel): NativeQueryBuilder {
        val builder = NativeQuery.builder()

        val sortOptions = mutableListOf<SortOptions>()

        param.sortBy?.takeIf { it.isNotBlank() }?.let { sortByValue ->
            // ARTIK declaredFields YERİNE getAllFields KULLANIYORUZ
            val fields = getAllFields(clazz)
            val sortFields = sortByValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val sortOrderParam = param.getSortOrderValue().uppercase()
            val sortOrders = sortOrderParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            sortFields.forEachIndexed { index, sortField ->
                val parts = sortField.split(".")
                val rootFieldName = parts[0]

                val rootField = findField(fields, rootFieldName)

                if (rootField != null) {
                    val directionStr = if (index < sortOrders.size) sortOrders[index] else sortOrderParam
                    val order = if (directionStr.equals("DESC", true)) SortOrder.Desc else SortOrder.Asc

                    val isNested = isNestedField(rootField)
                    var finalSortField = sortField

                    if (!isNested) {
                        // Derinlemesine alan bulma (Nested olmayanlar için)
                        val leafField = findDeepField(clazz, sortField)
                        val isText = leafField != null && isTextField(leafField)

                        // Eğer alan Text ise ve .keyword yoksa ekle
                        if (isText && !sortField.endsWith(".keyword")) {
                            finalSortField = "$sortField.keyword"
                        }
                    }

                    sortOptions.add(
                        SortOptions.of { s ->
                            s.field { f ->
                                f.field(finalSortField).order(order)
                                if (isNested) {
                                    f.nested { n -> n.path(rootFieldName) }
                                }
                                f
                            }
                        }
                    )
                }
            }
        }

        if (sortOptions.isNotEmpty()) {
            builder.withSort(sortOptions)
        } else {
            builder.withSort(
                SortOptions.of { s -> s.field { f -> f.field("pk").order(SortOrder.Desc) } }
            )
        }

        return builder
    }

    /**
     * YENİ METOD: Sadece o sınıfın değil, tüm üst sınıfların alanlarını getirir.
     * Bu sayede "title" gibi miras alınan alanlar artık bulunabilir.
     */
    private fun getAllFields(type: Class<*>): List<ReflectField> {
        return fieldCache.getOrPut(type) {
            val fields = mutableListOf<ReflectField>()
            var currentClass: Class<*>? = type
            while (currentClass != null && currentClass != Any::class.java) {
                fields.addAll(currentClass.declaredFields)
                currentClass = currentClass.superclass
            }
            fields
        }
    }

    private fun getCachedFieldName(field: ReflectField): String? {
        return columnNameCache.getOrPut(field) {
            field.getAnnotation(Field::class.java)?.name ?: ""
        }.takeIf { it.isNotEmpty() }
    }

    private fun findField(fields: List<ReflectField>, fieldName: String): ReflectField? {
        return fields.firstOrNull { field ->
            field.name == fieldName || getCachedFieldName(field) == fieldName
        }
    }

    private fun findDeepField(rootClass: Class<*>, path: String): ReflectField? {
        val parts = path.split(".")
        var currentClass = rootClass
        var currentField: ReflectField? = null

        for (part in parts) {
            if (part == "keyword") continue

            // YENİ: Burada da getAllFields kullanıyoruz
            val fields = getAllFields(currentClass)
            currentField = findField(fields, part) ?: return null

            val type = currentField.genericType
            currentClass = if (type is ParameterizedType) {
                val actualType = type.actualTypeArguments[0]
                if (actualType is Class<*>) actualType else currentField.type
            } else {
                currentField.type
            }
        }
        return currentField
    }

    private fun isNestedField(field: ReflectField): Boolean {
        val annotation = field.getAnnotation(Field::class.java)
        return annotation?.type == FieldType.Nested
    }

    private fun isTextField(field: ReflectField): Boolean {
        val annotation = field.getAnnotation(Field::class.java)
        // Eğer anotasyon yoksa ama tip String ise Text kabul et (Güvenlik önlemi)
        if (annotation == null) {
            return field.type == String::class.java
        }
        return annotation.type == FieldType.Text || annotation.type == FieldType.Auto
    }

    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun createIdQueryInternal(fieldPrefix: String?, id: String): Query {
        val (key, value) = idKeyAndValue(id)
        val fieldName = if (fieldPrefix != null)
            "$fieldPrefix.${normalizeString(key.name)}"
        else
            normalizeString(key.name)

        return Query.of { q ->
            q.term { t ->
                t.field(fieldName).value(toFieldValue(value))
            }
        }
    }

    private fun createIdsQueryInternal(fieldPrefix: String?, ids: List<String>): Query? {
        val (pkValues, skValues) = partitionIds(ids)
        val pkField = if (fieldPrefix != null) "$fieldPrefix.pk" else "pk"
        val skField = if (fieldPrefix != null) "$fieldPrefix.sk" else "sk"

        val hasPk = pkValues.isNotEmpty()
        val hasSk = skValues.isNotEmpty()

        if (!hasPk && !hasSk) return null

        return if (hasPk && hasSk) {
            Query.of { q ->
                q.bool { b ->
                    b.minimumShouldMatch("1")
                    b.should { s ->
                        s.terms { t ->
                            t.field(pkField).terms { v -> v.value(pkValues.map { FieldValue.of(it) }) }
                        }
                    }
                    b.should { s ->
                        s.terms { t ->
                            t.field(skField).terms { v -> v.value(skValues.map { toFieldValue(it) }) }
                        }
                    }
                }
            }
        } else if (hasPk) {
            Query.of { q ->
                q.terms { t -> t.field(pkField).terms { v -> v.value(pkValues.map { FieldValue.of(it) }) } }
            }
        } else {
            Query.of { q ->
                q.terms { t -> t.field(skField).terms { v -> v.value(skValues.map { toFieldValue(it) }) } }
            }
        }
    }

    private fun partitionIds(ids: List<String>): Pair<List<Long>, List<Any>> {
        val pkValues = mutableListOf<Long>()
        val skValues = mutableListOf<Any>()
        ids.forEach { id ->
            id.toLongOrNull()?.let { pkValues.add(it) } ?: skValues.add(LK(id))
        }
        return pkValues to skValues
    }

    private fun toFieldValue(value: Any): FieldValue {
        return when (value) {
            is String -> FieldValue.of(value)
            is Long -> FieldValue.of(value)
            is Int -> FieldValue.of(value.toLong())
            is Double -> FieldValue.of(value)
            is Boolean -> FieldValue.of(value)
            else -> FieldValue.of(value.toString())
        }
    }

    fun idQuery(id: String): Query {
        return createIdQueryInternal(null, id)
    }

    fun idQuery(field: String, id: String): Query {
        return createIdQueryInternal(field, id)
    }

    fun idQuery(builder: BoolQuery.Builder, id: String): BoolQuery.Builder {
        return builder.filter(createIdQueryInternal(null, id))
    }

    fun idQuery(builder: BoolQuery.Builder, field: String, id: String): BoolQuery.Builder {
        return builder.filter(createIdQueryInternal(field, id))
    }

    fun idsQuery(builder: BoolQuery.Builder, ids: List<String>): BoolQuery.Builder {
        createIdsQueryInternal(null, ids)?.let {
            builder.filter(it)
        }
        return builder
    }

    fun idsQuery(builder: BoolQuery.Builder, field: String, ids: List<String>): BoolQuery.Builder {
        createIdsQueryInternal(field, ids)?.let {
            builder.filter(it)
        }
        return builder
    }
}