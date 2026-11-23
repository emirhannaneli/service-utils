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
    var page: Int = 1
        get() = field.coerceAtLeast(1)

    @Transient
    var size: Int = 10
        get() = field.coerceIn(1, maxSize.coerceAtLeast(1))

    /**
     * Returns a Pageable object based on the current page and size.
     * @return A Pageable object.
     */
    fun ofPageable(): Pageable = PageRequest.of(pageIndex(), size)

    /**
     * Returns a Pageable object based on the current page, size and the given sort.
     * @return A Pageable object.
     */
    fun ofPageable(sort: Sort): Pageable = PageRequest.of(pageIndex(), size, sort)

    /** Zero-based page index, capped so that `index * size` (the row offset) still fits an Int. */
    private fun pageIndex(): Int = (page - 1).coerceIn(0, Int.MAX_VALUE / size)

    companion object {
        const val DEFAULT_MAX_SIZE: Int = 1000

        /**
         * Upper bound applied to every requested page size, so a single request cannot load a whole
         * table. Configured through `lubble.pagination.max-size`.
         */
        @JvmStatic
        @Volatile
        var maxSize: Int = DEFAULT_MAX_SIZE
    }
}
