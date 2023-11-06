package net.lubble.util.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import net.lubble.util.helper.EnumHelper

/**
 * ParameterModel
 * @param search String?
 * @param page Int?
 * @param size Int?
 * @param sort String?
 * @param order String?
 * */
open class ParameterModel {
    var search: String? = null

    @Min(1)
    var page: Int? = 1

    @Min(1)
    @Max(200)
    var size: Int? = 10

    var sort: String? = null

    var order: String? = null
}

enum class SortOrder : EnumHelper {
    ASC {
        override val label: String = "sort.order.asc"
        override val value: String = "asc"
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
    },
    DESC {
        override val label: String = "sort.order.desc"
        override val value: String = "desc"
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
    }
}
