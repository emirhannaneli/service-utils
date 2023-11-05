package net.lubble.util.spec

import net.lubble.util.model.SortOrder
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.mongodb.core.query.Query;

open class BaseSpec<T>(
    val base: T,
    val search: String?,
    val sort: String?,
    val order: SortOrder?,
) : PaginationSpec() {

    interface JPAModel<T> {
        fun ofSearch(): Specification<T>
    }

    interface MongoModel {
        fun ofSearch(): Query
    }
}