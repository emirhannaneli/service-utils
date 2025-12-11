package net.lubble.util.projection

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Selection
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.*

interface LJPAProjection<T : BaseModel> {
    private val manager: EntityManager
        get() = AppContextUtil.bean(EntityManager::class.java)

    fun findOne(spec: BaseSpec.JPA<T>): Optional<T> {
        val clazz = spec.clazz
        validateEntity(clazz)
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(clazz)
        val root = query.from(clazz)

        // Apply Predicates (Search)
        val predicate = spec.ofSearch().toPredicate(root, query, builder)
        query.where(predicate)

        // Entity Graph (Fetch Strategy)
        val entityGraph = createDynamicEntityGraph(spec, clazz)

        val typedQuery = manager.createQuery(query)
        typedQuery.maxResults = 1

        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        return typedQuery.resultList.firstOrNull()
            ?.let { Optional.of(it) }
            ?: Optional.empty()
    }

    fun findAll(spec: BaseSpec.JPA<T>, pagination: Boolean = true): Page<T> {
        val clazz = spec.clazz
        validateEntity(clazz)

        if (!spec.fields.isNullOrEmpty()) {
            return fetchWithProjection(spec, clazz, pagination)
        }

        var totalCount: Long = 0
        if (pagination) {
            val countQuery = manager.createQuery(count(spec))
            totalCount = countQuery.singleResult
            if (totalCount == 0L) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
        }

        val ids = if (pagination) {
            val cb = manager.criteriaBuilder
            
            val idQuery = cb.createTupleQuery()
            val root = idQuery.from(clazz)
            
            val predicate = spec.ofSearch().toPredicate(root, idQuery, cb)
            
            idQuery.orderBy(emptyList())
            
            val selections = mutableListOf<Selection<*>>(root.get<Any>("id").alias("id"))
            
            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val joinMapForSort = mutableMapOf<String, Join<*, *>>()
                val orders = pageable.sort.map { order ->
                    val path = getOrCreatePath(root, order.property, joinMapForSort)
                    selections.add(path)
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                }.toList()
                idQuery.orderBy(orders)
            }
            
            idQuery.where(predicate)
            idQuery.multiselect(selections)
            idQuery.distinct(true)

            val typedIdQuery = manager.createQuery(idQuery)
            typedIdQuery.firstResult = pageable.pageNumber * pageable.pageSize
            typedIdQuery.maxResults = pageable.pageSize
            
            val fetchedTuples = typedIdQuery.resultList
            if (fetchedTuples.isEmpty()) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
            
            fetchedTuples.map { it.get(0) as String }
        } else {
            emptyList()
        }

        val cb = manager.criteriaBuilder
        val query = cb.createQuery(clazz)
        val root = query.from(clazz)

        if (pagination) {
            query.where(root.get<String>("id").`in`(ids))
        } else {
            val predicate = spec.ofSearch().toPredicate(root, query, cb)
            query.where(predicate)
            query.distinct(true)

            val pageable = spec.ofSortedPageable()
            if (pageable.sort.isSorted) {
                val orders = pageable.sort.map { order ->
                    val path = getOrCreatePath(root, order.property)
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                }.toList()
                query.orderBy(orders)
            }
        }

        val typedQuery = manager.createQuery(query)

        val entityGraph = createDynamicEntityGraph(spec, clazz)
        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        var results = typedQuery.resultList

        if (pagination && ids.isNotEmpty()) {
            val entityMap = results.associateBy { it.getId() }
            results = ids.mapNotNull { entityMap[it] }
        }

        if (!pagination) {
            totalCount = results.size.toLong()
        }

        return PageImpl(results, spec.ofSortedPageable(), totalCount)
    }

    private fun createDynamicEntityGraph(spec: BaseSpec.JPA<T>, clazz: Class<T>): EntityGraph<T> {
        val entityGraph = manager.createEntityGraph(clazz)
        val requestedFields =
            spec.fields?.toSet() ?: return entityGraph


        requestedFields.forEach { fieldName ->
            val field = FieldUtils.getField(clazz, fieldName, true) ?: return@forEach

            if (field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java)
            ) {

                if (!entityGraph.attributeNodes.any { it.attributeName == fieldName }) {
                    entityGraph.addAttributeNodes(fieldName)
                }
            }
        }
        return entityGraph
    }

    private fun validateEntity(clazz: Class<*>): Unit {
        if (clazz == BaseModel::class.java || !isEntity(clazz)) {
            throw IllegalArgumentException(
                "Not an entity: ${clazz.name}. " +
                "BaseModel is a @MappedSuperclass and cannot be used directly in queries. " +
                "Please ensure your specification uses a concrete entity class that extends BaseModel."
            )
        }
    }
    
    fun count(spec: BaseSpec.JPA<T>): CriteriaQuery<Long> {
        val clazz = spec.clazz
        validateEntity(clazz)
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(clazz)
        val search = spec.ofSearch().toPredicate(root, query, builder)
        query.select(builder.countDistinct(root))
            .where(search)
        return query
    }
    
    private fun isEntity(clazz: Class<*>): Boolean {
        if (clazz.isAnnotationPresent(Entity::class.java)) {
            return true
        }
        
        return try {
            manager.metamodel.managedType(clazz)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    private fun fetchWithProjection(spec: BaseSpec.JPA<T>, clazz: Class<T>, pagination: Boolean): Page<T> {
        val cb = manager.criteriaBuilder

        var totalCount: Long = 0
        if (pagination) {
            val countQuery = manager.createQuery(count(spec))
            totalCount = countQuery.singleResult
            if (totalCount == 0L) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)
        }

        val tupleQuery = cb.createTupleQuery()
        val root = tupleQuery.from(clazz)

        val joinMap = mutableMapOf<String, Join<*, *>>()

        val requestedFields = spec.fields ?: emptyList()
        val selections = mutableListOf<Selection<*>>()

        selections.add(root.get<Any>("id").alias("id"))

        requestedFields.forEach { fieldPath ->
            try {
                val path = getOrCreatePath(root, fieldPath, joinMap)
                selections.add(path.alias(fieldPath))
            } catch (_: Exception) {
            }
        }

        tupleQuery.multiselect(selections)

        val predicate = spec.ofSearch().toPredicate(root, tupleQuery, cb)
        tupleQuery.where(predicate)

        val pageable = spec.ofSortedPageable()
        if (pageable.sort.isSorted) {
            val orders = pageable.sort.map { order ->
                // Sort alanları için de path çözümlemesi gerekebilir
                val path = getOrCreatePath(root, order.property, joinMap)
                if (order.isAscending) cb.asc(path) else cb.desc(path)
            }.toList()
            tupleQuery.orderBy(orders)
        }

        val typedQuery = manager.createQuery(tupleQuery)

        if (pagination) {
            typedQuery.firstResult = pageable.pageNumber * pageable.pageSize
            typedQuery.maxResults = pageable.pageSize
        }

        val tuples = typedQuery.resultList

        val results = tuples.map { tuple ->
            val entity = clazz.getDeclaredConstructor().newInstance()

            tuple.elements.forEach { tupleElement ->
                val fieldPath = tupleElement.alias
                val value = tuple.get(fieldPath)

                if (fieldPath != null && value != null) {
                    try {
                        writeNestedField(entity, fieldPath, value)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            entity
        }

        if (!pagination) totalCount = results.size.toLong()

        return PageImpl(results, pageable, totalCount)
    }

    /**
     * Verilen field path (örn: "category.subCategory.name") için gerekli Join'leri yapar
     * ve son path'i döner. JoinMap kullanarak mükerrer join'leri engeller.
     */
    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String,
        joinMap: MutableMap<String, Join<*, *>>
    ): jakarta.persistence.criteria.Path<Any> {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath)
        }

        val parts = fieldPath.split(".")
        var currentFrom: From<*, *> = root
        var currentPathKey = ""

        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            currentPathKey = if (currentPathKey.isEmpty()) part else "$currentPathKey.$part"

            if (joinMap.containsKey(currentPathKey)) {
                currentFrom = joinMap[currentPathKey] as From<*, *>
            } else {
                val join = currentFrom.join<Any, Any>(part, JoinType.LEFT)
                joinMap[currentPathKey] = join
                currentFrom = join
            }
        }

        return currentFrom.get(parts.last())
    }

    /**
     * Verilen field path için gerekli Join'leri yapar (joinMap olmadan).
     * Mevcut join'leri kontrol ederek mükerrer join'leri engeller.
     */
    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String
    ): jakarta.persistence.criteria.Path<Any> {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath)
        }

        val parts = fieldPath.split(".")
        var currentFrom: From<*, *> = root

        for (i in 0..<parts.size - 1) {
            val part = parts[i]
            
            val existingJoin = currentFrom.joins.firstOrNull { join ->
                join.attribute.name == part && join.joinType == JoinType.LEFT
            }

            if (existingJoin != null) {
                @Suppress("UNCHECKED_CAST")
                currentFrom = existingJoin as From<*, *>
            } else {
                currentFrom = currentFrom.join<Any, Any>(part, JoinType.LEFT)
            }
        }

        return currentFrom.get(parts.last())
    }

    /**
     * Reflection ile iç içe objeleri oluşturur ve değeri set eder.
     * Örn: target=Product, path="category.name", value="Electronics"
     * Bu metod Product içinde Category instance'ı yoksa oluşturur, sonra name'i set eder.
     */
    private fun writeNestedField(target: Any, path: String, value: Any) {
        if (!path.contains(".")) {
            FieldUtils.writeField(target, path, value, true)
            return
        }

        val parts = path.split(".")
        var currentObject = target

        for (i in 0 until parts.size - 1) {
            val fieldName = parts[i]
            val field = FieldUtils.getField(currentObject.javaClass, fieldName, true)

            var nestedObject = FieldUtils.readField(field, currentObject, true)

            if (nestedObject == null) {
                val fieldType = field.type
                nestedObject = fieldType.getDeclaredConstructor().newInstance()
                FieldUtils.writeField(field, currentObject, nestedObject, true)
            }
            currentObject = nestedObject
        }

        FieldUtils.writeField(currentObject, parts.last(), value, true)
    }
}