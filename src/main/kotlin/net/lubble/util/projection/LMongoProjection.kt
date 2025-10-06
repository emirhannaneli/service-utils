package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.*

/**
 * Interface for MongoDB projections.
 *
 * @param T the type of the model extending BaseModel
 */
interface LMongoProjection<T : BaseModel> {

    /**
     * Retrieves the MongoTemplate bean from the application context.
     *
     * @return the MongoTemplate bean
     */
    private val template: MongoTemplate
        get() = AppContextUtil.bean(MongoTemplate::class.java)

    /**
     * Finds a single document matching the given specification.
     *
     * @param spec the specification to search by
     * @return an Optional containing the found document, or empty if not found
     */
    fun findOne(spec: BaseSpec.Mongo<T>): Optional<T> {
        val clazz = spec.clazz
        val query = projection(spec)
        val result = template.findOne(query, clazz)
        return Optional.ofNullable(result)
    }

    /**
     * Finds all documents matching the given specification, with optional pagination.
     *
     * @param spec the specification to search by
     * @param pagination whether to apply pagination (default is true)
     * @return a Page containing the found documents
     */
    fun findAll(spec: BaseSpec.Mongo<T>, pagination: Boolean = true): Page<T> {
        val clazz = spec.clazz
        val query = projection(spec)
        val pageable = spec.ofSortedPageable()

        if (pagination) {
            query.skip(pageable.pageNumber * pageable.pageSize.toLong())
            query.limit(pageable.pageSize)
        }

        val result = template.find(query, clazz)
        val count = count(spec)
        return PageImpl(result, pageable, count)
    }

    /**
     * Checks if a document matching the given specification exists.
     *
     * @param spec the specification to search by
     * @return true if a document exists, false otherwise
     */
    fun exists(spec: BaseSpec.Mongo<T>): Boolean {
        val clazz = spec.clazz
        val query = spec.ofSearch()
        return template.exists(query, clazz)
    }

    /**
     * Deletes documents matching the given specification.
     *
     * @param spec the specification to search by
     */
    fun delete(spec: BaseSpec.Mongo<T>) {
        val clazz = spec.clazz
        val query = spec.ofSearch()
        template.remove(query, clazz)
    }

    /**
     * Creates a projection query based on the given specification and class.
     *
     * @param spec the specification to search by
     * @return a Query object with the projection applied
     */
    fun projection(spec: BaseSpec.Mongo<T>): Query {
        val clazz = spec.clazz
        val query = spec.ofSearch()
        val excludeFields = clazz.declaredFields.map { it.name }.toMutableSet()
        val includeFields =
            spec.fields?.map { it }?.toMutableSet() ?: clazz.declaredFields.map { it.name }.toMutableSet()
        includeFields.addAll(listOf("id", "pk", "sk", "deleted", "archived", "updatedAt", "createdAt"))

        excludeFields.removeAll(includeFields)

        includeFields.forEach {
            query.fields().include(it)
        }

        excludeFields.forEach {
            query.fields().exclude(it)
        }

        return query
    }

    /**
     * Counts the number of documents matching the given specification.
     *
     * @param spec the specification to search by
     * @return the count of matching documents
     */
    fun count(spec: BaseSpec.Mongo<T>): Long {
        val clazz = spec.clazz
        val query = spec.ofSearch()
        return template.count(query, clazz)
    }


}