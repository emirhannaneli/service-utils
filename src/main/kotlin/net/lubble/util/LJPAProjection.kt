package net.lubble.util

import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import net.lubble.util.spec.BaseSpec
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
interface LJPAProjection<T> {
    /**
     * Finds a single entity matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return an optional containing the entity if found, or empty if not found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> findOne(spec: BaseSpec<T>, clazz: Class<T>): Optional<T & Any> {
        val query = projection(spec, clazz) ?: return Optional.empty()
        val result = manager().createQuery(query).resultList
        return Optional.ofNullable(result[0] as T)
    }

    /**
     * Finds all entities matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a page of entities matching the specification.
     */
    fun <T> findAll(spec: BaseSpec<T>, clazz: Class<T>): Page<T> {
        val query = projection(spec, clazz) ?: return PageImpl(emptyList(), spec.ofPageable(), 0L)
        val result = manager().createQuery(query).resultList.map { tuple ->
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
     * Creates a projection query based on the given specification and entity class.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a criteria query for the projection.
     */
    private fun <T> projection(spec: BaseSpec<T>, clazz: Class<T>): CriteriaQuery<Tuple>? {
        val builder = manager().criteriaBuilder
        val query = builder.createTupleQuery()
        val root = query.from(clazz)
        val fields = spec.fields?.map { it }?.toMutableSet() ?: clazz.declaredFields.map { it.name }.toMutableSet()
        fields.addAll(listOf("pk", "sk", "updatedAt", "createdAt"))
        query.multiselect(fields.map { root.get<Any>(it).alias(it) })
            .where(spec.ofSearch().toPredicate(root, query, builder))
        return query
    }

    /**
     * Creates a count query based on the given specification and entity class.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a criteria query for counting the entities.
     */
    private fun <T> count(spec: BaseSpec<T>, clazz: Class<T>): CriteriaQuery<Long> {
        val builder = manager().criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        query.select(builder.count(query.from(clazz)))
            .where(spec.ofSearch().toPredicate(query.from(clazz), query, builder))
        return query
    }

    /**
     * Retrieves the entity manager from the application context.
     *
     * @return the entity manager.
     */
    private fun manager(): EntityManager = AppContextUtil.bean(EntityManager::class.java)
}

