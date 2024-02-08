package net.lubble.util.spec

import jakarta.persistence.Transient
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort


open class PaginationSpec {
    @Transient
    var page: Int? = 1
        get() = field?.coerceAtLeast(1)
    @Transient
    var size: Int? = 10
        get() = field?.coerceAtLeast(1)

    fun ofPageable(): Pageable {
        val page = (this.page?.minus(1) ?: 0).coerceAtLeast(0)
        val size = this.size?.coerceAtLeast(1) ?: 1
        return PageRequest.of(page, size)
    }

    fun ofPageable(sort: Sort) {
        val page = (this.page?.minus(1) ?: 0).coerceAtLeast(0)
        val size = this.size?.coerceAtLeast(1) ?: 1
        PageRequest.of(page, size, sort)
    }
}
