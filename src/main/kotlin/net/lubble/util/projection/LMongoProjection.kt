package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
     */
    fun findAll(spec: BaseSpec.Mongo<T>): List<T> {
        val clazz = spec.clazz
        val query = projection(spec)

        return template.find(query, clazz)
    }

    /**
     * Finds all documents matching the given specification, with pagination.
     *
     * @param spec the specification to search by
     * @param pageable the pagination information
     * @return a Page containing the found documents
     */
    fun findAll(spec: BaseSpec.Mongo<T>, pageable: Pageable): Page<T> {
        val clazz = spec.clazz
        val query = projection(spec)
        val total = template.count(query, clazz)
        query.with(pageable)
        val content = template.find(query, clazz)
        return PageImpl(content, pageable, total)
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
        val query = spec.ofSearch()

        if (spec.fields.isNullOrEmpty()) {
            return query
        }

        val fields = query.fields()
        val mandatoryFields = setOf("id", "pk", "sk", "deleted", "archived", "updatedAt", "createdAt")
        mandatoryFields.forEach { fields.include(it) }

        spec.fields?.forEach { fieldName ->
            fields.include(fieldName)
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