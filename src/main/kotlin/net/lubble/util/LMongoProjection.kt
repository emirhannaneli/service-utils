package net.lubble.util

import net.lubble.util.model.BaseMongoModel
import net.lubble.util.spec.BaseMongoSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.*

/**
 * Interface for MongoDB projections.
 *
 * @param T the type of the model extending BaseMongoModel
 */
interface LMongoProjection<T : BaseMongoModel> {

    /**
     * Finds a single document matching the given specification.
     *
     * @param spec the specification to search by
     * @param clazz the class of the document
     * @return an Optional containing the found document, or empty if not found
     */
    fun findOne(spec: BaseMongoSpec<T>, clazz: Class<T>): Optional<T> {
        val query = projection(spec, clazz)
        val result = template().findOne(query, clazz)
        return Optional.ofNullable(result)
    }

    /**
     * Finds all documents matching the given specification, with optional pagination.
     *
     * @param spec the specification to search by
     * @param clazz the class of the documents
     * @param pagination whether to apply pagination (default is true)
     * @return a Page containing the found documents
     */
    fun findAll(spec: BaseMongoSpec<T>, clazz: Class<T>, pagination: Boolean = true): Page<T> {
        val query = projection(spec, clazz)
        val pageable = spec.ofPageable()

        if (pagination) {
            query.skip(pageable.pageNumber * pageable.pageSize.toLong())
            query.limit(pageable.pageSize)
        }

        val result = template().find(query, clazz)
        val count = count(spec, clazz)
        return PageImpl(result, pageable, count)
    }

    /**
     * Creates a projection query based on the given specification and class.
     *
     * @param spec the specification to search by
     * @param clazz the class of the documents
     * @return a Query object with the projection applied
     */
    fun projection(spec: BaseMongoSpec<T>, clazz: Class<T>): Query {
        val query = spec.ofSearch()
        val excludeFields = clazz.declaredFields.map { it.name }.toMutableSet()
        val includeFields = spec.fields?.map { it }?.toMutableSet() ?: clazz.declaredFields.map { it.name }.toMutableSet()
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
     * @param clazz the class of the documents
     * @return the count of matching documents
     */
    fun count(spec: BaseMongoSpec<T>, clazz: Class<T>): Long {
        val query = spec.ofSearch()
        return template().count(query, clazz)
    }

    /**
     * Retrieves the MongoTemplate bean from the application context.
     *
     * @return the MongoTemplate bean
     */
    private fun template(): MongoTemplate = AppContextUtil.bean(MongoTemplate::class.java)
}