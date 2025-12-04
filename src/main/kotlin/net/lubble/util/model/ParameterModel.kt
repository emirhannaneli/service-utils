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
@JsonIgnoreProperties(value = ["page", "size", "sortBy", "sortOrder", "search", "isFiltering"])
open class ParameterModel : PaginationSpec() {
    @Transient
    var search: String? = null
        get() = field?.trim()?.lowercase()

    @Transient
    var sortBy: String? = null
        get() = field?.trim()

    @Transient
    var sortOrder: SortOrder = SortOrder.ASC
    
    /**
     * Returns the sort order as a string, supporting comma-separated values.
     * If sortOrderString is provided, it will be used; otherwise, the enum value will be used.
     */
    @Transient
    var sortOrderString: String? = null
        get() = field?.trim()?.uppercase()
    
    /**
     * Gets the sort order value as string, supporting comma-separated values.
     * This method prioritizes sortOrderString if provided, otherwise uses the enum value.
     */
    fun getSortOrderValue(): String {
        return sortOrderString ?: sortOrder.value
    }

    val isFiltering: Boolean
        @Transient
        get() {
            if (!search.isNullOrBlank()) return true

            var clazz: Class<*>? = this::class.java
            while (clazz != null && clazz != ParameterModel::class.java && clazz != Any::class.java) {
                for (field in clazz.declaredFields) {
                    if (java.lang.reflect.Modifier.isStatic(field.modifiers) || field.isSynthetic || field.name.startsWith("$")) continue
                    try {
                        field.isAccessible = true
                        val value = field.get(this)
                        if (value != null) {
                            if (value is String && value.isBlank()) continue
                            return true
                        }
                    } catch (e: Exception) {
                        // Ignore reflection errors
                    }
                }
                clazz = clazz.superclass
            }
            return false
        }
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