package net.lubble.util.spec

import net.lubble.util.model.ParameterModel
import org.springframework.data.domain.Example
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.mongodb.core.query.Query

open class BaseSpec<T : ParameterModel>(base: T) : ParameterModel() {

    init {
        this.page = base.page
        this.size = base.size
        this.search = base.search
        this.sort = base.sort
        this.order = base.order
    }

    interface JPAModel<T> {
        fun ofSearch(): Specification<T>
    }

    interface MongoModel<T> {
        fun ofSearch(): Query

        fun ofExample(): Example<T>
    }
}