package net.lubble.util.spec.tool

import net.lubble.util.LK
import net.lubble.util.model.BaseModel
import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Field as ReflectField

interface ElasticTool<T : BaseModel> {

    enum class IDType { PK, SK }

    companion object {
        private val fieldCache = ConcurrentHashMap<Class<*>, Map<String, ReflectField>>()

        private fun normalizeString(str: String): String {
            return str.lowercase(Locale.ENGLISH)
        }
    }

    val clazz: Class<T>
    var id: String?
    var ids: List<String>?
    var deleted: Boolean?
    var archived: Boolean?

    fun ofSearch(): Criteria

    fun defaultCriteria(): Criteria {
        val root = Criteria()

        id?.takeIf { it.isNotBlank() }?.let {
            root.subCriteria(createIdCriteriaInternal(null, it))
        }

        ids?.takeIf { it.isNotEmpty() }?.let {
            val idsCrit = createIdsCriteriaInternal(null, it)
            if (idsCrit != null) root.subCriteria(idsCrit)
        }

        deleted?.let { root.subCriteria(Criteria("deleted").`is`(it)) }
        archived?.let { root.subCriteria(Criteria("archived").`is`(it)) }

        return root
    }

    fun buildQuery(param: ParameterModel,criteria: Criteria): CriteriaQuery {
        val query = CriteriaQuery(criteria)
        val orders = mutableListOf<Sort.Order>()

        param.sortBy?.takeIf { it.isNotBlank() }?.let { sortByValue ->
            val sortFields = sortByValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val sortOrderParam = param.getSortOrderValue().uppercase()
            val sortOrders = sortOrderParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            sortFields.forEachIndexed { index, sortField ->
                val resolvedField = resolveSortField(clazz, sortField)

                if (resolvedField != null) {
                    val directionStr = if (index < sortOrders.size) sortOrders[index] else sortOrders.lastOrNull() ?: "ASC"
                    val direction = try {
                        Sort.Direction.valueOf(directionStr)
                    } catch (_: IllegalArgumentException) {
                        Sort.Direction.ASC
                    }
                    orders.add(Sort.Order(direction, resolvedField))
                }
            }
        }

        if (orders.isNotEmpty()) {
            query.addSort(Sort.by(orders))
        } else {
            query.addSort<CriteriaQuery>(Sort.by(Sort.Direction.DESC, "pk"))
        }

        return query
    }

    private fun resolveSortField(currentClass: Class<*>, path: String): String? {
        val predefined = setOf("pk", "sk", "createdAt", "updatedAt", "id")
        if (predefined.contains(path)) return path

        if (path.contains(".")) {
            val parts = path.split(".", limit = 2)
            val currentPart = parts[0]
            val remainingPart = parts[1]

            val field = findFieldInCache(currentClass, currentPart) ?: return null
            val resolvedRemaining = resolveSortField(field.type, remainingPart) ?: return null

            return "$currentPart.$resolvedRemaining"
        }

        val field = findFieldInCache(currentClass, path) ?: return null

        val elasticField = field.getAnnotation(Field::class.java)

        if (elasticField != null && elasticField.type == FieldType.Keyword) {
            return path
        }

        return if (field.type == String::class.java) {
            "$path.keyword"
        } else {
            path
        }
    }

    private fun getCachedFields(targetClass: Class<*>): Map<String, ReflectField> {
        return fieldCache.getOrPut(targetClass) {
            val fieldMap = mutableMapOf<String, ReflectField>()
            var current = targetClass
            while (current != Any::class.java) {
                current.declaredFields.forEach { field ->
                    fieldMap.putIfAbsent(field.name, field)
                    val annotation = field.getAnnotation(Field::class.java)
                    if (annotation != null && annotation.name.isNotBlank()) {
                        fieldMap.putIfAbsent(annotation.name, field)
                    }
                }
                current = current.superclass
            }
            fieldMap
        }
    }

    private fun findFieldInCache(targetClass: Class<*>, name: String): ReflectField? {
        return getCachedFields(targetClass)[name]
    }

    private fun idKeyAndValue(id: String): Pair<IDType, Any> {
        val value = id.toLongOrNull() ?: LK(id)
        val key = if (value is Long) IDType.PK else IDType.SK
        return key to value
    }

    private fun createIdCriteriaInternal(fieldPrefix: String?, id: String): Criteria {
        val (key, value) = idKeyAndValue(id)
        val fieldName = if (fieldPrefix != null)
            "$fieldPrefix.${normalizeString(key.name)}"
        else
            normalizeString(key.name)

        return Criteria(fieldName).`is`(value)
    }

    private fun createIdsCriteriaInternal(fieldPrefix: String?, ids: List<String>): Criteria? {
        if (ids.isEmpty()) return null

        val (pkValues, skValues) = partitionIds(ids)
        val pkField = if (fieldPrefix != null) "$fieldPrefix.pk" else "pk"
        val skField = if (fieldPrefix != null) "$fieldPrefix.sk" else "sk"

        val hasPk = pkValues.isNotEmpty()
        val hasSk = skValues.isNotEmpty()

        return when {
            hasPk && hasSk -> Criteria().or(Criteria(pkField).`in`(pkValues)).or(Criteria(skField).`in`(skValues))
            hasPk -> Criteria(pkField).`in`(pkValues)
            hasSk -> Criteria(skField).`in`(skValues)
            else -> null
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

    fun idCriteria(id: String): Criteria = createIdCriteriaInternal(null, id)
    fun idCriteria(criteria: Criteria, id: String): Criteria = criteria.subCriteria(createIdCriteriaInternal(null, id))
    fun idCriteria(field: String, id: String): Criteria = createIdCriteriaInternal(field, id)
    fun idCriteria(criteria: Criteria, field: String, id: String): Criteria = criteria.subCriteria(createIdCriteriaInternal(field, id))

    fun idsCriteria(criteria: Criteria, ids: List<String>): Criteria {
        createIdsCriteriaInternal(null, ids)?.let { return criteria.subCriteria(it) }
        return criteria
    }
    fun idsCriteria(criteria: Criteria, field: String, ids: List<String>): Criteria {
        createIdsCriteriaInternal(field, ids)?.let { return criteria.subCriteria(it) }
        return criteria
    }
}