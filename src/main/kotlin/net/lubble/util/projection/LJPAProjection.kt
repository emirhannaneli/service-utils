package net.lubble.util.projection

import jakarta.persistence.*
import jakarta.persistence.criteria.*
import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.lang.reflect.Field
import java.util.*
import java.util.stream.Collectors

interface LJPAProjection<T : BaseModel> {
    private val manager: EntityManager
        get() = AppContextUtil.bean(EntityManager::class.java)

    fun findOne(spec: BaseSpec.JPA<T>): Optional<T> {
        val clazz = spec.clazz
        validateEntity(clazz)
        val builder = manager.criteriaBuilder
        val query = builder.createQuery(clazz)
        val root = query.from(clazz)

        val predicate = spec.ofSearch().toPredicate(root, query, builder)
        query.where(predicate)

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

    fun findAll(spec: BaseSpec.JPA<T>): Page<T> {
        val clazz = spec.clazz
        validateEntity(clazz)

        if (!spec.fields.isNullOrEmpty()) {
            return fetchWithProjection(spec, clazz, true)
        }

        val countQuery = manager.createQuery(count(spec))
        val totalCount = countQuery.singleResult
        if (totalCount == 0L) return PageImpl(emptyList(), spec.ofSortedPageable(), 0L)

        val ids = fetchIdsForPagination(spec, clazz)
        if (ids.isEmpty()) return PageImpl(emptyList(), spec.ofSortedPageable(), totalCount)

        val results = fetchEntitiesByIds(ids, spec, clazz)

        val entityMap = results.associateBy { it.getId() }
        val sortedResults = ids.mapNotNull { entityMap[it] }

        return PageImpl(sortedResults, spec.ofSortedPageable(), totalCount)
    }

    fun fetchAll(spec: BaseSpec.JPA<T>): Collection<T> {
        val clazz = spec.clazz
        validateEntity(clazz)

        if (!spec.fields.isNullOrEmpty()) {
            return fetchWithProjection(spec, clazz, false).content
        }

        val cb = manager.criteriaBuilder
        val query = cb.createQuery(clazz)
        val root = query.from(clazz)

        val predicate = spec.ofSearch().toPredicate(root, query, cb)
        query.where(predicate)

        val pageable = spec.ofSortedPageable()
        if (pageable.sort.isSorted) {
            val orders = pageable.sort.stream().map { order ->
                try {
                    val path = getOrCreatePath(root, order.property)
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                } catch (_: Exception) {
                    null
                }
            }.filter { it != null }.collect(Collectors.toList())

            if (orders.isNotEmpty()) {
                query.orderBy(orders)
            }
        }

        val entityGraph = createDynamicEntityGraph(spec, clazz)
        val typedQuery = manager.createQuery(query)
        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        return typedQuery.resultList
    }

    private fun fetchIdsForPagination(spec: BaseSpec.JPA<T>, clazz: Class<T>): List<String> {
        val cb = manager.criteriaBuilder
        val idQuery = cb.createTupleQuery()
        val root = idQuery.from(clazz)
        val pageable = spec.ofSortedPageable()

        val predicate = spec.ofSearch().toPredicate(root, idQuery, cb)

        val selections = mutableListOf<Selection<*>>(root.get<Any>("id").alias("id"))
        val joinMapForSort = mutableMapOf<String, Join<*, *>>()

        if (pageable.sort.isSorted) {
            val orders = pageable.sort.stream().map { order ->
                try {
                    val path = getOrCreatePath(root, order.property, joinMapForSort)

                    selections.add(path)

                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                } catch (_: Exception) {
                    null
                }
            }.filter { it != null }.collect(Collectors.toList())

            if (orders.isNotEmpty()) {
                idQuery.orderBy(orders)
            }
        } else {
            idQuery.orderBy(emptyList())
        }

        idQuery.where(predicate)
        idQuery.multiselect(selections)

        val typedIdQuery = manager.createQuery(idQuery)
        typedIdQuery.firstResult = pageable.pageNumber * pageable.pageSize
        typedIdQuery.maxResults = pageable.pageSize

        val fetchedTuples = typedIdQuery.resultList
        return fetchedTuples.map { it.get(0) as String }
    }

    private fun fetchEntitiesByIds(ids: List<String>, spec: BaseSpec.JPA<T>, clazz: Class<T>): List<T> {
        val cb = manager.criteriaBuilder
        val query = cb.createQuery(clazz)
        val root = query.from(clazz)

        query.where(root.get<String>("id").`in`(ids))

        val entityGraph = createDynamicEntityGraph(spec, clazz)
        val typedQuery = manager.createQuery(query)
        if (entityGraph.attributeNodes.isNotEmpty()) {
            typedQuery.setHint("jakarta.persistence.fetchgraph", entityGraph)
        }

        return typedQuery.resultList
    }

    private fun createDynamicEntityGraph(spec: BaseSpec.JPA<T>, clazz: Class<T>): EntityGraph<T> {
        val entityGraph = manager.createEntityGraph(clazz)

        val joins = spec.joins ?: emptyList()
        val fields = spec.fields ?: emptyList()

        fields.forEach { fieldName ->
            val field = FieldUtils.getField(clazz, fieldName, true)
            if (field != null && isAssociation(field)) {
                if (entityGraph.attributeNodes.none { it.attributeName == fieldName }) {
                    entityGraph.addAttributeNodes(fieldName)
                }
            }
        }

        val sortedJoins = joins.sortedBy { it.length }

        val subgraphMap = mutableMapOf<String, Subgraph<*>>()

        sortedJoins.forEach { path ->
            addFetchPath(entityGraph, path, subgraphMap)
        }

        return entityGraph
    }



    private fun addFetchPath(
        graph: EntityGraph<T>,
        path: String,
        subgraphMap: MutableMap<String, Subgraph<*>>
    ) {
        if (!path.contains(".")) {
            if (graph.attributeNodes.none { it.attributeName == path }) {
                graph.addAttributeNodes(path)
            }
            return
        }

        val parts = path.split(".")
        var currentParentKey = ""


        for (i in 0..<parts.size - 1) {
            val part = parts[i]
            val nextPart = parts[i+1]

            val currentKey = if (currentParentKey.isEmpty()) part else "$currentParentKey.$part"

            var currentSubgraph = subgraphMap[currentKey]

            if (currentSubgraph == null) {
                if (i == 0) {
                    currentSubgraph = graph.addSubgraph<Any>(part)
                } else {
                    val parentSubgraph = subgraphMap[currentParentKey]
                        ?: throw IllegalStateException("Parent path '$currentParentKey' not found for '$path'. Ensure joins are sorted.")
                    currentSubgraph = parentSubgraph.addSubgraph<Any>(part)
                }
                subgraphMap[currentKey] = currentSubgraph
            }

            if (i == parts.size - 2) {
                if (currentSubgraph.attributeNodes.none { it.attributeName == nextPart }) {
                    currentSubgraph.addAttributeNodes(nextPart)
                }
            }

            currentParentKey = currentKey
        }
    }

    private fun isAssociation(field: Field): Boolean {
        return field.isAnnotationPresent(ManyToOne::class.java) ||
                field.isAnnotationPresent(OneToOne::class.java) ||
                field.isAnnotationPresent(OneToMany::class.java) ||
                field.isAnnotationPresent(ManyToMany::class.java)
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
        } catch (_: IllegalArgumentException) {
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
            val orders = pageable.sort.stream().map { order ->
                try {
                    val path = getOrCreatePath(root, order.property, joinMap)
                    if (order.isAscending) cb.asc(path) else cb.desc(path)
                } catch (e: Exception) {
                    null
                }
            }.filter { it != null }.collect(Collectors.toList())

            if (orders.isNotEmpty()) {
                tupleQuery.orderBy(orders)
            }
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

    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String,
        joinMap: MutableMap<String, Join<*, *>>
    ): Path<Any> {
        if (!fieldPath.contains(".")) {
            return root.get(fieldPath)
        }

        val parts = fieldPath.split(".")
        var currentFrom: From<*, *> = root
        var currentPathKey = ""

        for (i in 0..<parts.size - 1) {
            val part = parts[i]
            currentPathKey = if (currentPathKey.isEmpty()) part else "$currentPathKey.$part"

            if (joinMap.containsKey(currentPathKey)) {
                @Suppress("UNCHECKED_CAST")
                currentFrom = joinMap[currentPathKey] as From<*, *>
            } else {
                val join = currentFrom.join<Any, Any>(part, JoinType.LEFT)
                joinMap[currentPathKey] = join
                currentFrom = join
            }
        }

        return currentFrom.get(parts.last())
    }

    private fun getOrCreatePath(
        root: Root<*>,
        fieldPath: String
    ): Path<Any> {
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


    private fun writeNestedField(target: Any, path: String, value: Any) {
        if (!path.contains(".")) {
            FieldUtils.writeField(target, path, value, true)
            return
        }

        val parts = path.split(".")
        var currentObject = target

        for (i in 0..<parts.size - 1) {
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