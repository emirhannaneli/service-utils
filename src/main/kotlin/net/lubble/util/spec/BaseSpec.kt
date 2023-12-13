package net.lubble.util.spec

import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Example
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mongodb.core.query.Query

open class BaseSpec(base: ParameterModel) : ParameterModel() {

    init {
        this.page = base.page
        this.size = base.size
        this.search = base.search
        this.sortBy = base.sortBy
        this.sortOrder = base.sortOrder
    }

    interface JPAModel<T> {
        fun ofSearch(): Specification<T>
    }

    interface MongoModel<T> {
        fun ofSearch(): Query

        fun ofExample(): Example<T>
    }
}