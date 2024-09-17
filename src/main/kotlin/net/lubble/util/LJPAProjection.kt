package net.lubble.util

import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import net.lubble.util.model.BaseJPAModel
import net.lubble.util.spec.BaseJPASpec
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.*

/**
 * Interface for JPA projections.
 *
 * @param T the type of the entity.
 */
@JvmDefaultWithCompatibility
interface LJPAProjection<T : BaseJPAModel> {
    /**
     * Finds a single entity matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return an optional containing the entity if found, or empty if not found.
     */
    fun findOne(spec: BaseJPASpec<T>, clazz: Class<T>): Optional<T> {
        val query = projection(spec, clazz) ?: return Optional.empty()
        val result = manager().createQuery(query).resultList
        if (result.isEmpty()) return Optional.empty()
        val tuple = result[0]
        val entity = clazz.getDeclaredConstructor().newInstance()
        val fields = FieldUtils.getAllFields(clazz)
        fields.filter { field -> field.name in tuple.elements.map { element -> element.alias } }.forEach { field ->
            field.isAccessible = true
            val value = tuple.get(field.name)
            field.set(entity, value)
        }
        return Optional.of(entity)
    }

    /**
     * Finds all entities matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a page of entities matching the specification.
     */
    fun findAll(spec: BaseJPASpec<T>, clazz: Class<T>, pagination: Boolean = true): Page<T> {
        val projection = projection(spec, clazz) ?: return PageImpl(emptyList(), spec.ofPageable(), 0L)
        val query = manager().createQuery(projection)

        if (pagination) {
            val pageable = spec.ofPageable()
            query.firstResult = pageable.pageNumber * pageable.pageSize
            query.maxResults = pageable.pageSize
        }

        val result = query.resultList.map { tuple ->
            val entity = clazz.getDeclaredConstructor().newInstance()
            val fields = FieldUtils.getAllFields(clazz)
            fields.filter { field -> field.name in tuple.elements.map { element -> element.alias } }.forEach { field ->
                field.isAccessible = true
                field.set(entity, tuple.get(field.name))
            }
            entity
        }
        val count = manager().createQuery(count(spec, clazz)).singleResult
        return PageImpl(result, spec.ofPageable(), count)
    }

    /**
     * Checks if an entity matching the given specification exists.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return true if an entity exists, false otherwise.
     */
    fun exists(spec: BaseJPASpec<T>, clazz: Class<T>): Boolean {
        val query = count(spec, clazz)
        return manager().createQuery(query).singleResult > 0
    }

    /**
     * Creates a projection query based on the given specification and entity class.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a criteria query for the projection.
     */
    private fun projection(spec: BaseJPASpec<T>, clazz: Class<T>): CriteriaQuery<Tuple>? {
        val builder = manager().criteriaBuilder
        val query = builder.createTupleQuery()
        val root = query.from(clazz)
        val fields = spec.fields?.map { it }?.toMutableSet() ?: clazz.declaredFields.map { it.name }.toMutableSet()
        fields.addAll(listOf("id", "pk", "sk", "deleted", "archived", "updatedAt", "createdAt"))
        val search = spec.ofSearch().toPredicate(root, query, builder)
        /*val tuple = builder.tuple(fields.map { root.get<Any>(it).alias(it) })
        query.select(tuple).where(search)*/
        query.multiselect(fields.map { root.get<Any>(it).alias(it) }).where(search)
        return query
    }

    /**
     * Creates a count query based on the given specification and entity class.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a criteria query for counting the entities.
     */
    private fun count(spec: BaseJPASpec<T>, clazz: Class<T>): CriteriaQuery<Long> {
        val builder = manager().criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(clazz)
        val search = spec.ofSearch().toPredicate(root, query, builder)
        query.select(builder.count(root))
            .where(search)
        query.orderBy()
        return query
    }

    /**
     * Retrieves the entity manager from the application context.
     *
     * @return the entity manager.
     */
    private fun manager(): EntityManager = AppContextUtil.bean(EntityManager::class.java)
}

