package net.lubble.util.projection

import net.lubble.util.AppContextUtil
import net.lubble.util.model.BaseModel
import net.lubble.util.spec.BaseSpec
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.get
import org.springframework.data.elasticsearch.core.multiGet
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder


interface LElasticProjection<T : BaseModel> {
    private val operations: ElasticsearchOperations
        get() = AppContextUtil.bean(ElasticsearchOperations::class.java)

    fun searchSimilar(spec: BaseSpec.Elastic<T>): Page<T> {
        val clazz = spec.clazz
        val pageable = spec.ofSortedPageable()
        val query = spec.ofSearch().let {
            CriteriaQueryBuilder(it.criteria)
                .withPageable(pageable)
                .build()
        }

        val searchHits = operations.search(query, clazz)
        val results = searchHits.map { it.content }.distinctBy { it.getId() }
        val count = operations.count(query, clazz)
        return PageImpl(results, pageable, count)
    }

    fun findOne(spec: BaseSpec.Elastic<T>): T? {
        val clazz = spec.clazz
        val query = spec.ofSearch().let {
            CriteriaQueryBuilder(it.criteria)
                .build()
        }
        return operations.search(query, clazz).map { it.content }.firstOrNull()
    }

    fun exists(spec: BaseSpec.Elastic<T>): Boolean {
        val clazz = spec.clazz
        val query = spec.ofSearch().let {
            CriteriaQueryBuilder(it.criteria)
                .withPageable(spec.ofSortedPageable())
                .build()
        }

        return operations.count(query, clazz) > 0
    }
}