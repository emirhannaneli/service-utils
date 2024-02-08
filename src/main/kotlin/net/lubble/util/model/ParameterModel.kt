package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Transient
import net.lubble.util.annotation.lower.Lower
import net.lubble.util.annotation.trim.Trim
import net.lubble.util.helper.EnumHelper
import net.lubble.util.spec.PaginationSpec

/**
 * ParameterModel
 * @param search String?
 * @param page Int?
 * @param size Int?
 * @param sortBy String?
 * @param order SortOrder?
 * */
@JsonIgnoreProperties(value = ["page", "size", "sort", "order" ,"search"])
open class ParameterModel : PaginationSpec() {
    @Trim
    @Lower
    @Transient
    var search: String? = null

    @Trim
    @Transient
    var sortBy: String? = null

    @Transient
    var sortOrder: SortOrder? = null
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
