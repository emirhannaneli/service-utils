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

            return FILTER_FIELDS.get(javaClass).any { field ->
                val value = field.get(this)
                value != null && !(value is String && value.isBlank())
            }
        }

    /**
     * `sortBy`/`sortOrder` as (field, descending) pairs in request order, restricted to what
     * [options] allow. Directions map to the requested fields by position; when there are fewer directions
     * than fields the last one applies to the rest, and anything that is not `DESC` sorts ascending.
     */
    internal fun sortEntries(options: SpecOptions?): List<Pair<String, Boolean>> {
        val requested = sortBy?.split(",")?.map { it.trim() }?.filterTo(LinkedHashSet()) { it.isNotEmpty() }
        if (requested.isNullOrEmpty()) return emptyList()

        // Directions are paired with the fields as requested, before filtering, so that dropping a
        // disallowed field does not shift the directions of the ones that remain.
        val directions = getSortOrderValue().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val descending = requested.withIndex().associate { (index, field) ->
            field to "DESC".equals(directions.getOrNull(index) ?: directions.lastOrNull(), ignoreCase = true)
        }

        val allowed = options?.sort?.filterAllowedFields(requested) ?: requested
        return allowed.map { it to descending.getValue(it) }
    }

    private companion object {
        /** Filter fields declared by subclasses of ParameterModel, resolved once per class. */
        private val FILTER_FIELDS = object : ClassValue<List<java.lang.reflect.Field>>() {
            override fun computeValue(type: Class<*>): List<java.lang.reflect.Field> =
                generateSequence(type) { it.superclass }
                    .takeWhile { it != ParameterModel::class.java && it != Any::class.java }
                    .flatMap { it.declaredFields.asSequence() }
                    .filter {
                        !java.lang.reflect.Modifier.isStatic(it.modifiers) && !it.isSynthetic &&
                                !it.name.startsWith("$") && it.trySetAccessible()
                    }
                    .toList()
        }
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