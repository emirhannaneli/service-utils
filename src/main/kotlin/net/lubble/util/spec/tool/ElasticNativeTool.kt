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
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Field as ReflectField

/**
 * ElasticNativeTool defines the specifications for ElasticSearch models using the Native Query Client.
 * Optimized for the new Elasticsearch Java Client (co.elastic.clients).
 */
interface ElasticNativeTool<T : BaseModel> {
    companion object {
        private val fieldCache = ConcurrentHashMap<Class<*>, Array<ReflectField>>()
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

    /**
     * Implementing classes must provide the specific search logic returning a Query object.
     * Usually a BoolQuery.
     */
    fun ofSearch(): Query

    /**
     * Returns the default query (filters) as a BoolQuery Builder.
     * Uses 'filter' context for exact matches (faster, no scoring).
     */
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

    /**
     * Builds the final NativeQuery with sorting and pagination applied.
     */
    fun buildNativeQuery(param: ParameterModel, baseQuery: Query): NativeQuery {
        val builder = NativeQuery.builder()
            .withQuery(baseQuery)

        val sortOptions = mutableListOf<SortOptions>()

        param.sortBy?.takeIf { it.isNotBlank() }?.let { sortByValue ->
            val fields = getCachedFields(clazz)
            val sortFields = sortByValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val sortOrderParam = param.getSortOrderValue().uppercase()
            val sortOrders = sortOrderParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            sortFields.forEachIndexed { index, sortField ->
                if (isValidSortField(fields, sortField)) {
                    val directionStr = if (index < sortOrders.size) sortOrders[index] else sortOrderParam
                    val order = try {
                        SortOrder.valueOf(directionStr) // "ASC" or "DESC"
                    } catch (e: IllegalArgumentException) {
                        SortOrder.Asc
                    }

                    sortOptions.add(
                        SortOptions.of { s ->
                            s.field { f -> f.field(sortField).order(order) }
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

        return builder.build()
    }

    private fun getCachedFields(clazz: Class<*>): Array<ReflectField> {
        return fieldCache.getOrPut(clazz) { clazz.declaredFields }
    }

    private fun getCachedFieldName(field: ReflectField): String? {
        return columnNameCache.getOrPut(field) {
            field.getAnnotation(Field::class.java)?.name ?: ""
        }.takeIf { it.isNotEmpty() }
    }

    private fun isValidSortField(fields: Array<ReflectField>, sortField: String): Boolean {
        return fields.any { field ->
            field.name == sortField || getCachedFieldName(field) == sortField
        }
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
            // Bool Query: Should (OR) context
            Query.of { q ->
                q.bool { b ->
                    b.minimumShouldMatch("1")
                    b.should { s -> s.terms { t -> t.field(pkField).terms { v -> v.value(pkValues.map { FieldValue.of(it) }) } } }
                    b.should { s -> s.terms { t -> t.field(skField).terms { v -> v.value(skValues.map { toFieldValue(it) }) } } }
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

    /**
     * Creates a standalone ID query.
     */
    fun idQuery(id: String): Query {
        return createIdQueryInternal(null, id)
    }

    fun idQuery(field: String, id: String): Query {
        return createIdQueryInternal(field, id)
    }

    /**
     * Appends ID query to an existing BoolQuery Builder (as Filter/Must).
     */
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