package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.*
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter

interface LElasticProjection<T : BaseModel> {

    val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    fun searchPaged(spec: BaseSpec.Elastic<T>): Page<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val query: CriteriaQuery = spec.buildQuery(spec.param, criteria)

        applySorting(query)
        applySourceFilter(query, spec.fields)

        val pageable = PageRequest.of(spec.param.page - 1, spec.param.size, query.sort ?: Sort.unsorted())
        query.setPageable<CriteriaQuery>(pageable)
        query.trackScores = true

        val hits = operations.search(query, clazz)

        @Suppress("UNCHECKED_CAST")
        val content = SearchHitSupport.unwrapSearchHits(hits) as List<T>

        return PageImpl(content, query.pageable, hits.totalHits)
    }

    fun search(spec: BaseSpec.Elastic<T>): List<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val query = spec.buildQuery(spec.param, criteria)

        query.setPageable<CriteriaQuery>(PageRequest.of(0, 10000))
        applySorting(query)
        applySourceFilter(query, spec.fields)
        query.trackScores = true

        val hits = operations.search(query, clazz)
        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(hits) as List<T>
    }

    fun search(spec: BaseSpec.Elastic<T>, pageable: Pageable): List<T> {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val query = spec.buildQuery(spec.param, criteria)

        val finalSort = if (pageable.sort.isSorted) {
            pageable.sort.and(Sort.by(Sort.Direction.DESC, "_score"))
        } else {
            (query.sort ?: Sort.unsorted()).and(Sort.by(Sort.Direction.DESC, "_score"))
        }

        query.addSort<CriteriaQuery>(finalSort)
        query.setPageable<CriteriaQuery>(PageRequest.of(pageable.pageNumber, pageable.pageSize, finalSort))
        query.trackScores = true

        applySourceFilter(query, spec.fields)

        val hits = operations.search(query, clazz)
        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.unwrapSearchHits(hits) as List<T>
    }

    fun findOne(spec: BaseSpec.Elastic<T>): T? {
        val clazz = spec.clazz
        val criteria = spec.ofSearch()

        val query = CriteriaQuery(criteria)
        query.setPageable<CriteriaQuery>(PageRequest.of(0, 1))

        applySorting(query)
        applySourceFilter(query, spec.fields)
        query.trackScores = true

        val hits = operations.search(query, clazz)
        return hits.searchHits.firstNotNullOfOrNull { it.content }
    }

    fun exists(spec: BaseSpec.Elastic<T>): Boolean {
        val clazz = spec.clazz
        val query = CriteriaQuery(spec.ofSearch())
        return operations.count(query, clazz) > 0
    }

    fun count(spec: BaseSpec.Elastic<T>): Long {
        val clazz = spec.clazz
        val query = CriteriaQuery(spec.ofSearch())
        return operations.count(query, clazz)
    }

    private fun applySorting(query: CriteriaQuery) {
        val currentSort = query.sort
        val scoreSort = Sort.by(Sort.Direction.DESC, "_score")

        if (currentSort != null && currentSort.isSorted) {
            query.addSort(currentSort.and(scoreSort))
        } else {
            query.addSort<CriteriaQuery>(scoreSort)
        }
    }

    private fun applySourceFilter(query: CriteriaQuery, fields: Collection<String>?) {
        val requiredFields = setOf("id", "pk", "sk", "createdAt", "updatedAt")

        if (!fields.isNullOrEmpty()) {
            val combinedFields = fields.toSet().plus(requiredFields).toTypedArray()
            query.addSourceFilter(FetchSourceFilter(null, combinedFields, null))
        }
    }
}