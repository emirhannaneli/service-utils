package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter

/**
 * Interface for performing Native Elasticsearch operations on entities of type T.
 * Optimized for 'co.elastic.clients' and BaseSpec.ElasticNative.
 *
 * @param T The type of the entity, which must extend BaseModel.
 */
interface LElasticNativeProjection<T : BaseModel> {

    /**
     * Provides an instance of ElasticsearchOperations.
     */
    private val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    /**
     * Searches for entities using Native Query (supports Boosting, Nested, etc.).
     *
     * @param spec The native specification containing the Query object.
     * @return A Page containing the search results.
     */
    fun searchPaged(spec: BaseSpec.ElasticNative<T>): Page<T> {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch() // Returns co.elastic.clients.elasticsearch._types.query_dsl.Query
        val pageable = spec.ofSortedPageable()

        val queryBuilder = NativeQuery.builder()
            .withQuery(elasticQuery)
            .withPageable(pageable)

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)
        val page = SearchHitSupport.searchPageFor(hits, pageable)

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(page) as Page<T>? ?: PageImpl(emptyList(), pageable, 0)
    }

    /**
     * Returns a list of all matching entities (Default limit: 10000).
     */
    fun search(spec: BaseSpec.ElasticNative<T>): List<T> {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val queryBuilder = NativeQuery.builder()
            .withQuery(elasticQuery)
            .withPageable(PageRequest.of(0, 10000))

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)

        @Suppress("UNCHECKED_CAST")
        return hits.searchHits.map { it.content }
    }

    /**
     * Returns a list of matching entities with specific pagination.
     */
    fun search(spec: BaseSpec.ElasticNative<T>, pageable: Pageable): List<T> {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val queryBuilder = NativeQuery.builder()
            .withQuery(elasticQuery)
            .withPageable(pageable)

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)

        @Suppress("UNCHECKED_CAST")
        return hits.searchHits.map { it.content }
    }

    /**
     * Finds a single entity matching the given specification.
     */
    fun findOne(spec: BaseSpec.ElasticNative<T>): T? {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val queryBuilder = NativeQuery.builder()
            .withQuery(elasticQuery)
            .withPageable(PageRequest.of(0, 1))

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        return operations.search(query, clazz).firstNotNullOfOrNull { it.content }
    }

    /**
     * Checks if any entity exists matching the given specification.
     */
    fun exists(spec: BaseSpec.ElasticNative<T>): Boolean {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val query = NativeQuery.builder()
            .withQuery(elasticQuery)
            .build()

        return operations.count(query, clazz) > 0
    }

    /**
     * Counts the number of entities matching the given Native Query specification.
     *
     * @param spec The native specification containing the Query object.
     * @return The total count of matching entities.
     */
    fun count(spec: BaseSpec.ElasticNative<T>): Long {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val query = NativeQuery.builder()
            .withQuery(elasticQuery)
            .build()

        return operations.count(query, clazz)
    }

    /**
     * Helper to apply source filtering (Include specific fields only).
     */
    private fun applySourceFilter(queryBuilder: NativeQueryBuilder, fields: Collection<String>?) {
        if (!fields.isNullOrEmpty()) {
            queryBuilder.withSourceFilter(
                FetchSourceFilter(null, fields.toTypedArray(), null)
            )
        }
    }
}