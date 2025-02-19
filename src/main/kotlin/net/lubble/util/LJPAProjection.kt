package net.lubble.util

import jakarta.persistence.EntityManager
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import net.lubble.util.model.BaseJPAModel
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
interface LJPAProjection<T : BaseJPAModel> {
    /**
     * Finds a single entity matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return an optional containing the entity if found, or empty if not found.
     */
    fun findOne(spec: BaseSpec.JPA<T>, clazz: Class<T>): Optional<T> {
        val query = projection(spec, clazz) ?: return Optional.empty()
        val result = manager().createQuery(query).resultList
        if (result.isEmpty()) return Optional.empty()
        val tuple = result[0]
        val entity = clazz.getDeclaredConstructor().newInstance()
        setFields(entity, tuple)
        return Optional.of(entity)
    }

    /**
     * Finds all entities matching the given specification.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a page of entities matching the specification.
     */
    fun findAll(spec: BaseSpec.JPA<T>, clazz: Class<T>, pagination: Boolean = true): Page<T> {
        val projection = projection(spec, clazz) ?: return PageImpl(emptyList(), spec.ofPageable(), 0L)
        val query = manager().createQuery(projection)

        if (pagination) {
            val pageable = spec.ofSortedPageable()
            query.firstResult = pageable.pageNumber * pageable.pageSize
            query.maxResults = pageable.pageSize
        }

        val entity = clazz.getDeclaredConstructor().newInstance()
        val result = query.resultList
            .map { tuple ->
                setFields(entity, tuple)
            entity
            }.distinctBy { it.getId() }
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
    fun exists(spec: BaseSpec.JPA<T>, clazz: Class<T>): Boolean {
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
    private fun projection(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Tuple>? {
        val builder = manager().criteriaBuilder
        val query = builder.createTupleQuery()
        val root = query.from(clazz)
        val fields = spec.fields?.map { it }?.toMutableSet() ?: FieldUtils.getAllFields(clazz).map { it.name }.toMutableSet()
        fields.addAll(listOf("id", "pk", "sk", "deleted", "archived", "updatedAt", "createdAt"))
        val search = spec.ofSearch().toPredicate(root, query, builder)
        query.multiselect(fields.map {
            val declaredField = FieldUtils.getField(clazz, it, true)

            if (declaredField.isAnnotationPresent(OneToMany::class.java) ||
                declaredField.isAnnotationPresent(ManyToOne::class.java) ||
                declaredField.isAnnotationPresent(OneToOne::class.java) ||
                declaredField.isAnnotationPresent(ManyToMany::class.java)
            ) {

                val join = root.join<Any, Any>(it, JoinType.LEFT)
                join.alias(it)

            } else {
                root.get<Any>(it).alias(it)
            }
        }).where(search)
        return query
    }

    /**
     * Creates a count query based on the given specification and entity class.
     *
     * @param spec the specification to filter entities.
     * @param clazz the class of the entity.
     * @return a criteria query for counting the entities.
     */
    private fun count(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Long> {
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

    @Suppress("UNCHECKED_CAST")
    private fun setFields(entity: T, tuple: Tuple) {
        val fields = FieldUtils.getAllFields(entity::class.java)
        fields.filter { field -> field.name in tuple.elements.map { element -> element.alias } }
            .forEach { field ->
                field.isAccessible = true
                val value = tuple.get(field.name)
                if (Collection::class.java.isAssignableFrom(field.type)) {
                    val collection = field.get(entity) as MutableCollection<Any>? ?: mutableListOf()
                    if (value != null)
                        collection.add(value)

                    field.set(entity, collection)
                } else {
                    field.set(entity, value)
                }
            }
    }
}

