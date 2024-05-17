package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector
import jakarta.persistence.Transient
import net.lubble.util.AppContextUtil
import net.lubble.util.helper.EnumHelper
import net.lubble.util.spec.PaginationSpec
import java.util.*

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

    fun toKey(): String {
        val mapper = AppContextUtil.bean(ObjectMapper::class.java).apply {
            setAnnotationIntrospector(object : JacksonAnnotationIntrospector() {
                override fun findPropertyIgnoralByName(config: MapperConfig<*>?, a: Annotated?): JsonIgnoreProperties.Value {
                    return JsonIgnoreProperties.Value.empty()
                }
            })
        }
        val json = mapper.writeValueAsString(this)
        return Base64.getEncoder().encodeToString(json.toByteArray())
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
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
    },
    DESC {
        override val label: String = "sort.order.desc"
        override val value: String = name.uppercase()
        override val color: String?
            get() = null
        override val icon: String?
            get() = null
    }
}