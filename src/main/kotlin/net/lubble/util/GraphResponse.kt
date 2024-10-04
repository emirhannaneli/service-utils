package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import net.lubble.util.model.ExceptionModel
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import java.util.*

/**
 * A class representing a response for a graph-related request.
 * The response contains a message, a code, and optional details.
 */
@JsonPropertyOrder("message", "code", "details")
class GraphResponse(
    @JsonProperty("message")
    val message: String? = null,
    @JsonProperty("code")
    val code: String? = null,
    @JsonProperty("details")
    val details: Any? = null,
) {

    /**
     * Constructs a GraphResponse with a message and code.
     * @param message The message to be included in the response.
     * @param code The code to be included in the response.
     */
    constructor(message: String, code: String?) : this(
        message = source().getMessage(message, null, locale()),
        code = code,
        details = null,
    )

    /**
     * Constructs a GraphResponse with a message.
     * @param message The message to be included in the response.
     */
    constructor(message: String) : this(
        message = source().getMessage(message, null, locale()),
        code = null
    )

    /**
     * Constructs a GraphResponse with a message and details.
     * @param message The message to be included in the response.
     * @param details Additional details to be included in the response.
     */
    constructor(message: String, details: Any) : this(
        message = source().getMessage(message, null, locale()),
        code = null,
        details = details,
    )

    /**
     * Constructs a GraphResponse from an ExceptionModel.
     * @param ex The ExceptionModel to be used for constructing the response.
     */
    constructor(ex: ExceptionModel) : this(
        message = source().getMessage(ex.message(), null, locale()),
        code = ex.code(),
    )

    /**
     * Constructs a GraphResponse from an ExceptionModel and additional details.
     * @param ex The ExceptionModel to be used for constructing the response.
     * @param details Additional details to be included in the response.
     */
    constructor(ex: ExceptionModel, details: Any) : this(
        message = source().getMessage(ex.message(), null, locale()),
        code = ex.code(),
        details = details,
    )

    companion object {
        /**
         * Creates a GraphPageResponse from a Page and a collection of data.
         * @param page The Page object containing pagination information.
         * @param data The collection of data to be included in the response.
         * @return A GraphPageResponse object.
         */
        fun of(page: Page<*>, data: Collection<*>): GraphPageResponse {
            return GraphPageResponse(page, data)
        }

        /**
         * Retrieves the MessageSource bean from the application context.
         * @return The MessageSource bean.
         */
        private fun source(): MessageSource {
            return AppContextUtil.bean(MessageSource::class.java)
        }

        /**
         * Retrieves the current locale from the LocaleContextHolder.
         * @return The current Locale.
         */
        private fun locale(): Locale {
            return LocaleContextHolder.getLocale()
        }
    }
}

/**
 * A class representing a paginated response for a graph-related request.
 * The response contains metadata about the pagination and the data itself.
 */
@JsonPropertyOrder("meta", "data")
data class GraphPageResponse(
    @JsonProperty("meta")
    val meta: Meta,
    @JsonProperty("data")
    val data: Collection<*>,
) {
    /**
     * Constructs a GraphPageResponse from a Page and a collection of data.
     * @param page The Page object containing pagination information.
     * @param data The collection of data to be included in the response.
     */
    constructor(page: Page<*>, data: Collection<*>) : this(
        meta = Meta(page),
        data = data,
    )

    /**
     * A class representing metadata about the pagination.
     */
    @JsonPropertyOrder("current", "size", "totalItems", "totalPages", "hasNext", "hasPrevious", "items")
    class Meta(
        @JsonProperty("current")
        val current: Int,
        @JsonProperty("size")
        val size: Int,
        @JsonProperty("totalItems")
        val totalItems: Long,
        @JsonProperty("totalPages")
        val totalPages: Int,
        @JsonProperty("hasNext")
        val hasNext: Boolean,
        @JsonProperty("hasPrevious")
        val hasPrevious: Boolean,
    ) {
        /**
         * Constructs Meta from a Page object.
         * @param page The Page object containing pagination information.
         */
        constructor(page: Page<*>) : this(
            current = page.number + 1,
            size = page.size,
            totalItems = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
            hasPrevious = page.hasPrevious(),
        )
    }
}