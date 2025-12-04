package net.lubble.util.spec.tool

import jakarta.persistence.Column
import jakarta.persistence.criteria.*
import net.lubble.util.LK
import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SortOrder
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.elasticsearch.core.query.Criteria as ElasticCriteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery as ElasticQuery
import org.springframework.data.mongodb.core.query.Criteria as MongoCriteria
import org.springframework.data.mongodb.core.query.Query as MongoQuery


/**
 * SpecTool class is used to define common specifications for JPA and MongoDB models.
 *
 * @property base The base parameter model.
 */
open class SpecTool(private val base: ParameterModel) {
    /**
     * Returns the pageable object from the base parameter model.
     */
    fun ofPageable() = base.ofPageable()

    fun ofPageable(sort: Sort) = base.ofPageable(sort)

    fun ofSortedPageable(): Pageable {
        val sort = if (base.sortBy != null) {
            val sortFields = base.sortBy!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val sortOrders = base.getSortOrderValue().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            if (sortFields.isEmpty()) {
                Sort.unsorted()
            } else {
                val sortOrdersList = sortFields.mapIndexed { index, field ->
                    val direction = if (index < sortOrders.size) {
                        try {
                            Sort.Direction.valueOf(sortOrders[index].uppercase())
                        } catch (e: IllegalArgumentException) {
                            Sort.Direction.valueOf(base.getSortOrderValue().uppercase())
                        }
                    } else {
                        Sort.Direction.valueOf(base.getSortOrderValue().uppercase())
                    }
                    Sort.Order(direction, field)
                }
                Sort.by(sortOrdersList)
            }
        } else {
            Sort.unsorted()
        }
        return base.ofPageable(sort)
    }

    /**
     * IDType enum class defines the types of ids.
     * */
    enum class IDType {
        PK, SK
    }

    enum class SearchType {
        EQUAL, STARTS_WITH, ENDS_WITH, LIKE
    }
}