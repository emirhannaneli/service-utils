package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import net.lubble.util.model.ExceptionModel
import org.springframework.context.MessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

/**
 * A class representing a response for a graph-related request.
 * The response contains a message, a code, and optional details.
 */
@JsonPropertyOrder("message", "code", "details")
class GraphResponse(
    @field:JsonProperty("message")
    val message: String? = null,
    @field:JsonProperty("code")
    val code: String? = null,
    @field:JsonProperty("details")
    val details: Any? = null,
) {

    /**
     * Constructs a GraphResponse with a message and code.
     * @param message The message to be included in the response.
     * @param code The code to be included in the response.
     */
    constructor(message: String, code: String?) : this(
        message = getLocalizedMessage(message),
        code = code,
        details = null,
    )

    /**
     * Constructs a GraphResponse with a message.
     * @param message The message to be included in the response.
     */
    constructor(message: String) : this(
        message = getLocalizedMessage(message),
        code = null
    )

    /**
     * Constructs a GraphResponse with a message and details.
     * @param message The message to be included in the response.
     * @param details Additional details to be included in the response.
     */
    constructor(message: String, details: Any) : this(
        message = getLocalizedMessage(message),
        code = null,
        details = details,
    )

    /**
     * Constructs a GraphResponse from an ExceptionModel.
     * @param ex The ExceptionModel to be used for constructing the response.
     */
    constructor(ex: ExceptionModel) : this(
        message = getLocalizedMessage(ex.message()),
        code = ex.code(),
    )

    /**
     * Constructs a GraphResponse from an ExceptionModel and additional details.
     * @param ex The ExceptionModel to be used for constructing the response.
     * @param details Additional details to be included in the response.
     */
    constructor(ex: ExceptionModel, details: Any) : this(
        message = getLocalizedMessage(ex.message()),
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
         * Creates a GraphPageResponse from a collection of data, a Pageable, and the total number of items.
         * @param data The collection of data to be included in the response.
         * @param pageable The Pageable object containing pagination information.
         * @param total The total number of items.
         * @return A GraphPageResponse object.
         */
        fun of(data: Collection<*>, pageable: Pageable, total: Long): GraphPageResponse {
            return GraphPageResponse(data, pageable, total)
        }

        /**
         * Gets localized message if Spring is available, otherwise returns the message as-is.
         * @param message The message key or text.
         * @return The localized message or the original message.
         */
        private fun getLocalizedMessage(message: String): String {
            if (SpringDetectionUtil.isSpringAvailable() && SpringDetectionUtil.isSpringMessageSourceAvailable()) {
                return try {
                    val source = source()
                    val locale = locale()
                    source.getMessage(message, null, locale)
                } catch (e: Exception) {
                    // If Spring is available but MessageSource is not configured, return message as-is
                    message
                }
            }
            return message
        }

        /**
         * Retrieves the MessageSource bean from the application context.
         * @return The MessageSource bean.
         * @throws IllegalStateException if Spring is not available.
         */
        private fun source(): MessageSource {
            return AppContextUtil.bean(MessageSource::class.java)
        }

        /**
         * Retrieves the current locale.
         * @return The current Locale.
         */
        private fun locale(): Locale {
            return if (SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
                try {
                    val localeHolderClass = Class.forName("org.springframework.context.i18n.LocaleContextHolder")
                    val getLocaleMethod = localeHolderClass.getMethod("getLocale")
                    @Suppress("UNCHECKED_CAST")
                    getLocaleMethod.invoke(null) as Locale
                } catch (e: Exception) {
                    Locale.getDefault()
                }
            } else {
                Locale.getDefault()
            }
        }
    }
}

/**
 * A class representing a paginated response for a graph-related request.
 * The response contains metadata about the pagination and the data itself.
 */
@JsonPropertyOrder("meta", "data")
data class GraphPageResponse(
    @field:JsonProperty("meta")
    val meta: Meta,
    @field:JsonProperty("data")
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
     * Constructs a GraphPageResponse from a collection of data, a Pageable, and the total number of items.
     * @param data The collection of data to be included in the response.
     * @param pageable The Pageable object containing pagination information.
     * @param total The total number of items.
     */
    constructor(data: Collection<*>, pageable: Pageable, total: Long) : this(
        meta = Meta(
            pageable = pageable,
            total = total
        ),
        data = data
    )

    /**
     * A class representing metadata about the pagination.
     */
    @JsonPropertyOrder("current", "size", "totalItems", "totalPages", "hasNext", "hasPrevious", "items")
    class Meta(
        @field:JsonProperty("current")
        val current: Int,
        @field:JsonProperty("size")
        val size: Int,
        @field:JsonProperty("totalItems")
        val totalItems: Long,
        @field:JsonProperty("totalPages")
        val totalPages: Int,
        @field:JsonProperty("hasNext")
        val hasNext: Boolean,
        @field:JsonProperty("hasPrevious")
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

        constructor(pageable: Pageable, total: Long) : this(
            current = pageable.pageNumber + 1,
            size = pageable.pageSize,
            totalItems = total,
            totalPages = if (pageable.pageSize == 0) 0 else ((total + pageable.pageSize - 1) / pageable.pageSize).toInt(),
            hasNext = (pageable.pageNumber + 1) * pageable.pageSize < total,
            hasPrevious = pageable.pageNumber > 0
        )
    }

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
         * Creates a GraphPageResponse from a collection of data, a Pageable, and the total number of items.
         * @param data The collection of data to be included in the response.
         * @param pageable The Pageable object containing pagination information.
         * @param total The total number of items.
         * @return A GraphPageResponse object.
         */
        fun of(data: Collection<*>, pageable: Pageable, total: Long): GraphPageResponse {
            return GraphPageResponse(data, pageable, total)
        }

        /**
         * Creates an empty GraphPageResponse.
         * @return An empty GraphPageResponse object.
         */
        fun empty(): GraphPageResponse {
            return GraphPageResponse(
                meta = Meta(
                    current = 0,
                    size = 0,
                    totalItems = 0,
                    totalPages = 0,
                    hasNext = false,
                    hasPrevious = false
                ),
                data = emptyList<Any>()
            )
        }
    }
}