package net.lubble.util.spec.tool

import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import net.lubble.util.spec.tool.SpecTool.IDType
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Field as ReflectField

/**
 * ElasticTool interface defines the specifications for ElasticSearch models.
 * Optimized for robustness and logical isolation of queries.
 */
interface ElasticTool<T : BaseModel> {
    companion object {
        // Reflection cache
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
     * Implementing classes must provide the specific search logic.
     */
    fun ofSearch(): Criteria

    /**
     * Returns the default query (filters) for an ElasticSearch model.
     * Uses a 'Root' container approach to prevent logical mixing.
     */
    fun defaultCriteria(param: ParameterModel): Criteria {
        // Boş bir kök oluşturuyoruz.
        val root = Criteria()

        // Her bir özellik, kendi başına izole bir kriter olarak eklenir.
        // Bu sayede "AND" mantığı kesinleşir.

        id?.let {
            // idCriteria artık 'root'u manipüle etmek yerine yeni bir kriter döndürüyor
            // ve biz onu root'a ekliyoruz (veya metodun yeni yapısını kullanıyoruz).
            root.subCriteria(createIdCriteriaInternal(null, it))
        }

        ids?.let {
            val idsCrit = createIdsCriteriaInternal(null, it)
            if (idsCrit != null) root.subCriteria(idsCrit)
        }

        deleted?.let {
            root.subCriteria(Criteria("deleted").`is`(it))
        }

        archived?.let {
            root.subCriteria(Criteria("archived").`is`(it))
        }

        return root
    }

    /**
     * Builds the final CriteriaQuery with sorting applied.
     */
    fun buildQuery(param: ParameterModel, criteria: Criteria): CriteriaQuery {
        val query = CriteriaQuery(criteria)
        val orders = mutableListOf<Sort.Order>()

        param.sortBy?.takeIf { it.isNotBlank() }?.let { sortByValue ->
            val fields = getCachedFields(clazz)
            val sortFields = sortByValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val sortOrderParam = param.getSortOrderValue().uppercase()
            val sortOrders = sortOrderParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            sortFields.forEachIndexed { index, sortField ->
                if (isValidSortField(fields, sortField)) {
                    // Yönü belirle (index out of bounds kontrolü ile)
                    val directionStr = if (index < sortOrders.size) sortOrders[index] else sortOrderParam

                    val direction = try {
                        Sort.Direction.valueOf(directionStr)
                    } catch (e: IllegalArgumentException) {
                        Sort.Direction.ASC
                    }

                    orders.add(Sort.Order(direction, sortField))
                }
            }
        }

        if (orders.isNotEmpty()) {
            query.addSort<CriteriaQuery>(Sort.by(orders))
        } else {
            query.addSort(Sort.by(Sort.Direction.DESC, "pk"))
        }

        return query
    }

    // region Reflection & Validation Helpers
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
    // endregion

    // region Criteria Construction Helpers (Internal)

    // Bu metodlar "helper" olarak ayrıldı, state değiştirmez, obje üretir.
    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun createIdCriteriaInternal(fieldPrefix: String?, id: String): Criteria {
        val (key, value) = idKeyAndValue(id)
        val fieldName = if (fieldPrefix != null)
            "$fieldPrefix.${Companion.normalizeString(key.name)}"
        else
            Companion.normalizeString(key.name)

        return Criteria(fieldName).`is`(value)
    }

    private fun createIdsCriteriaInternal(fieldPrefix: String?, ids: List<String>): Criteria? {
        val (pkValues, skValues) = partitionIds(ids)
        val pkField = if (fieldPrefix != null) "$fieldPrefix.pk" else "pk"
        val skField = if (fieldPrefix != null) "$fieldPrefix.sk" else "sk"

        val hasPk = pkValues.isNotEmpty()
        val hasSk = skValues.isNotEmpty()

        if (!hasPk && !hasSk) return null

        return if (hasPk && hasSk) {
            // OR bloğu oluştururken dikkatli oluyoruz
            Criteria().or(Criteria(pkField).`in`(pkValues))
                .or(Criteria(skField).`in`(skValues))
        } else if (hasPk) {
            Criteria(pkField).`in`(pkValues)
        } else {
            Criteria(skField).`in`(skValues)
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
    // endregion

    // region Public API - Backward Compatible but Safer

    /**
     * Creates a standalone ID criteria.
     */
    fun idCriteria(id: String): Criteria {
        return createIdCriteriaInternal(null, id)
    }

    /**
     * Appends ID criteria to an existing criteria as a SUB-CRITERIA (AND).
     * This is safer than chaining .and() directly on mixed logic.
     */
    fun idCriteria(criteria: Criteria, id: String): Criteria {
        return criteria.subCriteria(createIdCriteriaInternal(null, id))
    }

    fun idCriteria(field: String, id: String): Criteria {
        return createIdCriteriaInternal(field, id)
    }

    fun idCriteria(criteria: Criteria, field: String, id: String): Criteria {
        return criteria.subCriteria(createIdCriteriaInternal(field, id))
    }

    fun idsCriteria(criteria: Criteria, ids: List<String>): Criteria {
        createIdsCriteriaInternal(null, ids)?.let {
            return criteria.subCriteria(it)
        }
        return criteria
    }

    fun idsCriteria(criteria: Criteria, field: String, ids: List<String>): Criteria {
        createIdsCriteriaInternal(field, ids)?.let {
            return criteria.subCriteria(it)
        }
        return criteria
    }
    // endregion
}