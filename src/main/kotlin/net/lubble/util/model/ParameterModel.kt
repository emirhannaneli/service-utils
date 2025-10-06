package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Transient
import net.lubble.util.helper.EnumHelper
import net.lubble.util.spec.PaginationSpec

/**
 * This class represents a model for parameters.
 * It extends PaginationSpec and provides additional fields for search, sortBy, and sortOrder.
 * It also provides methods to get and set these fields.
 * @property search The search string. It is transient, meaning it is not persisted in the database.
 * @property sortBy The field to sort by. It is transient, meaning it is not persisted in the database.
 * @property sortOrder The order to sort in. It is transient, meaning it is not persisted in the database.
 */
@JsonIgnoreProperties(value = ["page", "size", "sortBy", "sortOrder", "search"])
open class ParameterModel : PaginationSpec() {
    @Transient
    var search: String? = null
        get() = field?.trim()?.lowercase()

    @Transient
    var sortBy: String? = null
        get() = field?.trim()

    @Transient
    var sortOrder: SortOrder = SortOrder.ASC
}

/**
 * This enum represents the order to sort in.
 * It provides labels, values, colors, and icons for each order.
 */
enum class SortOrder : EnumHelper {
    ASC {
        override val label: String = "sort.order.asc"
        override val value: String = name.uppercase()
    },
    DESC {
        override val label: String = "sort.order.desc"
        override val value: String = name.uppercase()
    }
}