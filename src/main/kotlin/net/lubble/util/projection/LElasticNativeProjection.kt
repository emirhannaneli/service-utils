package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.*
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

    private val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    /**
     * Searches for entities using Native Query (supports Boosting, Nested, etc.).
     */
    fun searchPaged(spec: BaseSpec.ElasticNative<T>): Page<T> {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val originalPageable = spec.ofPageable()

        val scoreSort = Sort.by(Sort.Direction.DESC, "_score")

        val finalSort = scoreSort.and(originalPageable.sort)

        val pageable = spec.ofPageable(finalSort)

        val queryBuilder = spec.nativeQueryBuilder(spec.param)
            .withQuery(elasticQuery)
            .withPageable(pageable)
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)
        val page = SearchHitSupport.searchPageFor(hits, pageable)

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(page) as Page<T>? ?: PageImpl(emptyList(), pageable, 0)
    }

    fun search(spec: BaseSpec.ElasticNative<T>): List<T> {
        return search(spec, PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "_score")))
    }

    fun search(spec: BaseSpec.ElasticNative<T>, pageable: Pageable): List<T> {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val queryBuilder = spec.nativeQueryBuilder(spec.param)
            .withQuery(elasticQuery)
            .withPageable(pageable)
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)

        return hits.searchHits.map { it.content }
    }

    fun findOne(spec: BaseSpec.ElasticNative<T>): T? {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val queryBuilder = NativeQuery.builder()
            .withQuery(elasticQuery)
            .withPageable(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "_score")))
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields)

        val query = queryBuilder.build()
        return operations.search(query, clazz).firstNotNullOfOrNull { it.content }
    }

    fun exists(spec: BaseSpec.ElasticNative<T>): Boolean {
        val clazz = spec.clazz
        val elasticQuery = spec.ofSearch()

        val query = NativeQuery.builder()
            .withQuery(elasticQuery)
            .build()

        return operations.count(query, clazz) > 0
    }

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
        val requiredFields = setOf("id", "pk", "sk", "createdAt", "updatedAt")

        val combinedFields = if (fields.isNullOrEmpty()) {
            requiredFields
        } else {
            fields.toSet().plus(requiredFields)
        }

        if (combinedFields.isNotEmpty()) {
            queryBuilder.withSourceFilter(
                FetchSourceFilter(null, combinedFields.toTypedArray(), arrayOf())
            )
        }
    }
}