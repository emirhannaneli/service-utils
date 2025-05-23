package net.lubble.util

import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.util.*
import kotlin.reflect.full.memberProperties

interface LJPAProjection<T : BaseModel> {
    fun findOne(spec: BaseSpec.JPA<T>, clazz: Class<T>): Optional<T> {
        val query = projection(spec, clazz) ?: return Optional.empty()
        return manager().createQuery(query).resultList
            .firstOrNull()
            ?.let { tuple ->
                clazz.getDeclaredConstructor().newInstance().apply {
                    setFields(this, tuple)
                }
            }
            ?.let { Optional.of(it) }
            ?: Optional.empty()
    }

    fun findAll(spec: BaseSpec.JPA<T>, clazz: Class<T>, pagination: Boolean = true): Page<T> {
        val projection = projection(spec, clazz) ?: return PageImpl(emptyList(), spec.ofPageable(), 0L)
        val query = manager().createQuery(projection)

        if (pagination) {
            val pageable = spec.ofSortedPageable()
            query.firstResult = pageable.pageNumber * pageable.pageSize
            query.maxResults = pageable.pageSize
        }

        val results = query.resultList.map { tuple ->
            clazz.getDeclaredConstructor().newInstance().apply {
                setFields(this, tuple)
            }
        }.distinctBy { it.getId() }

        val count = manager().createQuery(count(spec, clazz)).singleResult
        return PageImpl(results, spec.ofPageable(), count)
    }

    fun exists(spec: BaseSpec.JPA<T>, clazz: Class<T>): Boolean =
        manager().createQuery(count(spec, clazz)).singleResult > 0

    private fun projection(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Tuple>? {
        val builder = manager().criteriaBuilder
        val query = builder.createTupleQuery()
        val root = query.from(clazz)

        val requiredFields = setOf("id", "pk", "sk", "deleted", "archived", "updatedAt", "createdAt")
        val fields = (spec.fields?.toSet() ?: clazz.kotlin.memberProperties.map { it.name }.toSet())
            .plus(requiredFields)

        val selections = fields.map { fieldName ->
            val field = FieldUtils.getField(clazz, fieldName, true)

            when {
                field.isAnnotationPresent(OneToMany::class.java) ||
                        field.isAnnotationPresent(ManyToOne::class.java) ||
                        field.isAnnotationPresent(OneToOne::class.java) ||
                        field.isAnnotationPresent(ManyToMany::class.java) -> {
                    root.join<Any, Any>(fieldName, JoinType.LEFT).apply {
                        alias(fieldName)
                    }
                }

                else -> root.get<Any>(fieldName).alias(fieldName)
            }
        }

        val search = spec.ofSearch().toPredicate(root, query, builder)
        return query.select(builder.tuple(selections)).where(search)
    }

    private fun count(spec: BaseSpec.JPA<T>, clazz: Class<T>): CriteriaQuery<Long> {
        val builder = manager().criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(clazz)
        val search = spec.ofSearch().toPredicate(root, query, builder)
        return query.select(builder.count(root))
            .where(search)
            .apply { orderBy() }
    }

    private fun manager(): EntityManager = AppContextUtil.bean(EntityManager::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun setFields(entity: T, tuple: Tuple) {
        val tupleAliases = tuple.elements.mapTo(HashSet()) { it.alias }

        FieldUtils.getAllFields(entity::class.java)
            .filter { it.name in tupleAliases }
            .forEach { field ->
                field.isAccessible = true
                val value = tuple.get(field.name)

                when {
                    Collection::class.java.isAssignableFrom(field.type) -> {
                        val collection = (field.get(entity) as? MutableCollection<Any>) ?: mutableListOf()
                        value?.let { collection.add(it) }
                        field.set(entity, collection)
                    }

                    else -> field.set(entity, value)
                }
            }
    }
}