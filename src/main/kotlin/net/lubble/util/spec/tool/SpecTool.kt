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
        val sort = if (base.sortBy != null) {
            val sortFields = base.sortBy!!.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
            val filteredFields = options?.sort?.filterAllowedFields(sortFields) ?: sortFields
            val sortOrders = base.getSortOrderValue().split(",").map { it.trim() }.filter { it.isNotEmpty() }

            if (filteredFields.isEmpty()) {
                Sort.unsorted()
            } else {
                val sortOrdersList = filteredFields.mapIndexed { index, field ->
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
        return ofPageable(sort)
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