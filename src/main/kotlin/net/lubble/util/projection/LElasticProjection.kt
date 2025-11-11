package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter

/**
 * Interface for performing Elasticsearch operations on entities of type T.
 *
 * @param T The type of the entity, which must extend BaseModel.
 */
interface LElasticProjection<T : BaseModel> {

    /**
     * Provides an instance of ElasticsearchOperations.
     */
    private val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    /**
     * Searches for entities similar to the given specification.
     *
     * @param spec The specification containing search criteria, pageable, and entity class.
     * @return A Page containing the search results.
     */
    fun searchPaged(spec: BaseSpec.Elastic<T>): Page<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()
        val pageable = spec.ofSortedPageable()

        val query = CriteriaQueryBuilder(criteria)
            .withPageable(pageable)
            .withSourceFilter(
                FetchSourceFilter(
                    null,
                    spec.fields?.toTypedArray(),
                    null
                )
            )
            .build()

        val hits = operations.search(query, clazz)
        val page = SearchHitSupport.searchPageFor(hits, pageable)

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(page) as Page<T>? ?: PageImpl(emptyList(), pageable, 0)
    }

    fun search(spec: BaseSpec.Elastic<T>): List<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val query = CriteriaQueryBuilder(criteria)
            .withPageable(Pageable.unpaged())
            .withSourceFilter(
                FetchSourceFilter(
                    null,
                    spec.fields?.toTypedArray(),
                    null
                )
            )
            .build()

        val hits = operations.search(query, clazz)
        @Suppress("UNCHECKED_CAST")
        return hits.searchHits.map { it.content }
    }

    /**
     * Finds a single entity matching the given specification.
     *
     * @param spec The specification containing search criteria and entity class.
     * @return The first matching entity, or null if no match is found.
     */
    fun findOne(spec: BaseSpec.Elastic<T>): T? {
        val clazz = spec.clazz
        val query = spec.ofSearch().let {
            CriteriaQueryBuilder(it)
                .build()
        }
        return operations.search(query, clazz).firstNotNullOfOrNull { it.content }
    }

    /**
     * Checks if any entity exists matching the given specification.
     *
     * @param spec The specification containing search criteria and entity class.
     * @return True if at least one entity matches the criteria, false otherwise.
     */
    fun exists(spec: BaseSpec.Elastic<T>): Boolean {
        val clazz = spec.clazz
        val query = spec.ofSearch().let {
            CriteriaQueryBuilder(it)
                .build()
        }

        return operations.count(query, clazz) > 0
    }
}