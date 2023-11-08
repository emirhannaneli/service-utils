package net.lubble.util.model

import net.lubble.util.helper.EnumHelper
import net.lubble.util.spec.PaginationSpec

/**
 * ParameterModel
 * @param search String?
 * @param page Int?
 * @param size Int?
 * @param sort String?
 * @param order String?
 * */
open class ParameterModel : PaginationSpec() {
    var search: String? = null

    var sort: String? = null

    var order: SortOrder? = null
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
