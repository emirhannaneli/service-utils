package net.lubble.util.spec

import jakarta.persistence.Transient
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * This class represents a specification for pagination.
 * It contains information about the current page and the size of the page.
 * @property page The current page number. It is transient, meaning it is not persisted in the database.
 * @property size The size of the page. It is transient, meaning it is not persisted in the database.
 */
open class PaginationSpec {
    @Transient
    var page: Int? = 1
        get() = field?.coerceAtLeast(1)
    @Transient
    var size: Int? = 10
        get() = field?.coerceAtLeast(1)

    /**
     * Converts this PaginationSpec into a Pageable.
     * @return The Pageable representing this PaginationSpec.
     */
    fun ofPageable(): Pageable {
        val page = (this.page?.minus(1) ?: 0).coerceAtLeast(0)
        val size = this.size?.coerceAtLeast(1) ?: 1
        return PageRequest.of(page, size)
    }

    /**
     * Converts this PaginationSpec into a Pageable with a specific sort order.
     * @param sort The sort order to apply.
     */
    fun ofPageable(sort: Sort) {
        val page = (this.page?.minus(1) ?: 0).coerceAtLeast(0)
        val size = this.size?.coerceAtLeast(1) ?: 1
        PageRequest.of(page, size, sort)
    }
}