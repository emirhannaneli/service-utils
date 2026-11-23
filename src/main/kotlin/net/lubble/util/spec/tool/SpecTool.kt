package net.lubble.util.spec.tool

import net.lubble.util.model.ParameterModel
import net.lubble.util.model.SpecOptions
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort


/**
 * SpecTool class is used to define common specifications for JPA and MongoDB models.
 *
 * @property base The base parameter model.
 */
open class SpecTool(private val base: ParameterModel) {
    open val options: SpecOptions? = null

    /**
     * Returns the pageable object from the base parameter model.
     */
    fun ofPageable() = base.ofPageable()

    fun ofPageable(sort: Sort) = base.ofPageable(sort)

    fun ofSortedPageable(): Pageable {
        val orders = base.sortEntries(options).map { (field, descending) ->
            Sort.Order(if (descending) Sort.Direction.DESC else Sort.Direction.ASC, field)
        }
        return ofPageable(if (orders.isEmpty()) Sort.unsorted() else Sort.by(orders))
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