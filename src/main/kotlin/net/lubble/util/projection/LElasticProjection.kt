package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.*
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

    // ... (Diğer tüm metodlar ve özellikler aynı kalır)

    private val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    /**
     * Searches for entities similar to the given specification.
     * Prioritizes relevance (_score) even if a sort is provided.
     */
    fun searchPaged(spec: BaseSpec.Elastic<T>): Page<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val originalPageable = spec.ofSortedPageable()

        val scoreSort = Sort.by(Sort.Direction.DESC, "_score")

        val finalSort = scoreSort.and(originalPageable.sort)

        val pageable = PageRequest.of(
            originalPageable.pageNumber,
            originalPageable.pageSize,
            finalSort
        )

        val queryBuilder = CriteriaQueryBuilder(criteria)
            .withPageable(pageable)
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields) // Burası değişmedi

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)
        val page = SearchHitSupport.searchPageFor(hits, pageable)

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(page) as Page<T>? ?: PageImpl(emptyList(), pageable, 0)
    }

    fun search(spec: BaseSpec.Elastic<T>): List<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "_score"))

        val queryBuilder = CriteriaQueryBuilder(criteria)
            .withPageable(pageable)
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields) // Burası değişmedi

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)

        @Suppress("UNCHECKED_CAST")
        return hits.searchHits.map { it.content }
    }

    fun search(spec: BaseSpec.Elastic<T>, pageable: Pageable): List<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val scoreSort = Sort.by(Sort.Direction.DESC, "_score")
        val finalSort = scoreSort.and(pageable.sort)

        val finalPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort)

        val queryBuilder = CriteriaQueryBuilder(criteria)
            .withPageable(finalPageable)
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields) // Burası değişmedi

        val query = queryBuilder.build()
        val hits = operations.search(query, clazz)

        @Suppress("UNCHECKED_CAST")
        return hits.searchHits.map { it.content }
    }

    /**
     * Finds a single entity matching the given specification.
     * Returns the one with the highest Score (best match).
     */
    fun findOne(spec: BaseSpec.Elastic<T>): T? {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val queryBuilder = CriteriaQueryBuilder(criteria)
            .withPageable(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "_score")))
            .withTrackScores(true)

        applySourceFilter(queryBuilder, spec.fields) // Burası değişmedi

        val query = queryBuilder.build()
        return operations.search(query, clazz).firstNotNullOfOrNull { it.content }
    }

    fun exists(spec: BaseSpec.Elastic<T>): Boolean {
        val clazz = spec.clazz
        val query = CriteriaQueryBuilder(spec.ofSearch())
            .build()

        return operations.count(query, clazz) > 0
    }

    fun count(spec: BaseSpec.Elastic<T>): Long {
        val clazz = spec.clazz
        val query = CriteriaQueryBuilder(spec.ofSearch())
            .build()

        return operations.count(query, clazz)
    }

    private fun applySourceFilter(queryBuilder: CriteriaQueryBuilder, fields: Collection<String>?) {
        val requiredFields = setOf("id", "pk", "sk", "createdAt", "updatedAt")

        val combinedFields = if (fields.isNullOrEmpty()) {
            requiredFields
        } else {
            fields.toSet().plus(requiredFields)
        }

        if (combinedFields.isNotEmpty()) {
            queryBuilder.withSourceFilter(
                FetchSourceFilter(null, combinedFields.toTypedArray(), null)
            )
        }
    }
}