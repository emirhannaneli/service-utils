package net.lubble.util.spec

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort


open class PaginationSpec {
    private val page: Int = 1
    private val size: Int = 10

    fun ofPageable(): Pageable {
        val page = (this.page - 1).coerceAtLeast(0)
        val size = this.size.coerceAtLeast(1)
        return PageRequest.of(page, size)
    }

    fun ofPageable(sort: Sort) {
        val page = (this.page - 1).coerceAtLeast(0)
        val size = this.size.coerceAtLeast(1)
        PageRequest.of(page, size, sort)
    }
}
