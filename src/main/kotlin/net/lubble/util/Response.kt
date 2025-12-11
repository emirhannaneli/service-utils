package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import net.lubble.util.model.ExceptionModel
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import java.util.*

/**
 * This class represents a response that can be sent to the client.
 * It contains information about the status of the request, a message, a code, and additional details.
 * @property message The message to be sent to the client.
 * @property status The HTTP status of the response.
 * @property code An optional code that can provide additional information about the response.
 * @property details Any additional details that should be included in the response.
 */
@JsonPropertyOrder("message", "status", "code", "details")
class Response() {
    @field:JsonProperty("message")
    private lateinit var message: String

    @field:JsonProperty("status")
    private var status: HttpStatus = OK

    @field:JsonProperty("code")
    private var code: String? = null

    @field:JsonProperty("details")
    private var details: Any? = null

    /**
     * Constructs a new Response from a message, status, code, and details.
     * @param message The message to include in the Response.
     * @param status The status to include in the Response.
     * @param code An optional code that can provide additional information about the response.
     * @param details Any additional details that should be included in the response.
     */
    constructor(message: String, status: HttpStatus, code: String?, details: Any?) : this() {
        this.message = source().getMessage(message, null, locale())
        this.status = status
        this.code = code
        this.details = details
    }

    /**
     * Constructs a new Response from a message and status.
     * @param message The message to include in the Response.
     * @param status The status to include in the Response.
     */
    constructor(message: String, status: HttpStatus) : this() {
        this.message = source().getMessage(message, null, locale())
        this.status = status
    }

    /**
     * Constructs a new Response from a message.
     * @param message The message to include in the Response.
     */
    constructor(message: String) : this() {
        this.message = source().getMessage(message, null, locale())
    }

    /**
     * Constructs a new Response from a message and additional details.
     * @param message The message to include in the Response.
     * @param details The additional details to include in the Response.
     */
    constructor(message: String, details: Any) : this() {
        this.message = source().getMessage(message, null, locale())
        this.details = details
    }

    /**
     * Constructs a new Response from a message, status, and details.
     * @param message The message to include in the Response.
     * @param status The status to include in the Response.
     * @param details The additional details to include in the Response.
     */
    constructor(message: String, status: HttpStatus, details: Any) : this() {
        this.message = source().getMessage(message, null, locale())
        this.status = status
        this.details = details
    }

    /**
     * Constructs a new Response from an ExceptionModel.
     * @param ex The ExceptionModel to construct the Response from.
     */
    constructor(ex: ExceptionModel) : this() {
        this.message = ex.message()
        this.status = ex.status()
        this.code = ex.code()
    }

    /**
     * Constructs a new Response from an ExceptionModel and additional details.
     * @param ex The ExceptionModel to construct the Response from.
     * @param details The additional details to include in the Response.
     */
    constructor(ex: ExceptionModel, details: Any) : this() {
        this.message = ex.message()
        this.status = ex.status()
        this.code = ex.code()
        this.details = details
    }

    /**
     * Handles the Response for a servlet.
     * @param response The HttpServletResponse to handle.
     */
    fun servletHandler(response: HttpServletResponse) {
        response.status = status.value()
        response.writer.write(mapper().writeValueAsString(this))
        response.characterEncoding = "UTF-8"
        response.contentType = "application/json;charset=UTF-8;"
        response.writer.flush()
    }

    fun build(): ResponseEntity<Response> {
        return ResponseEntity(this, status)
    }

    companion object {
        /**
         * Constructs a new Response from a Page and a list of data.
         * @param page The Page to construct the Response from.
         * @param data The data to include in the Response.
         * @return The constructed Response.
         */
        fun of(page: Page<*>, data: Collection<*>): ResponseEntity<PageResponse> {
            return PageResponse(
                current = page.number + 1,
                size = page.size,
                totalItems = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                items = data
            ).build()
        }

        /**
         * Constructs a new Response from a collection of data, a Pageable, and the total number of items.
         * @param data The data to include in the Response.
         * @param pageable The Pageable to construct the Response from.
         * @param total The total number of items.
         * @return The constructed Response.
         */
        fun of(data: Collection<*>, pageable: Pageable, total: Long): ResponseEntity<PageResponse> {
            return PageResponse(
                current = pageable.pageNumber + 1,
                size = pageable.pageSize,
                totalItems = total,
                totalPages = if (pageable.pageSize == 0) 0 else ((total + pageable.pageSize - 1) / pageable.pageSize).toInt(),
                hasNext = (pageable.pageNumber + 1) * pageable.pageSize < total,
                hasPrevious = pageable.pageNumber > 0,
                items = data
            ).build()
        }

        /**
         * Retrieves the MessageSource bean from the application context.
         * @return The MessageSource bean.
         */
        private fun source(): MessageSource {
            return AppContextUtil.bean(MessageSource::class.java)
        }

        /**
         * Retrieves the current locale.
         * @return The current locale.
         */
        private fun locale(): Locale {
            return LocaleContextHolder.getLocale()
        }

        /**
         * Retrieves the ObjectMapper bean from the application context.
         * @return The ObjectMapper bean.
         */
        private fun mapper(): ObjectMapper {
            return AppContextUtil.bean(ObjectMapper::class.java)
        }
    }
}

/**
 * This class represents a page response that can be sent to the client.
 * It contains information about the current page, the size of the page, the total number of items, the total number of pages, whether there are next and previous pages, and the items on the page.
 * @property current The current page number.
 * @property size The size of the page.
 * @property totalItems The total number of items.
 * @property totalPages The total number of pages.
 * @property hasNext Whether there is a next page.
 * @property hasPrevious Whether there is a previous page.
 * @property items The items on the page.
 */
@JsonPropertyOrder("current", "size", "totalItems", "totalPages", "hasNext", "hasPrevious", "items")
data class PageResponse(
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
    @JsonProperty("items")
    val items: Collection<*>
) {
    /**
     * Constructs a new PageResponse from a Page and a list of data.
     * @param page The Page to construct the PageResponse from.
     * @param data The data to include in the PageResponse.
     */
    constructor(page: Page<*>, data: Collection<*>) : this(
        current = page.number + 1,
        size = page.size,
        totalItems = page.totalElements,
        totalPages = page.totalPages,
        hasNext = page.hasNext(),
        hasPrevious = page.hasPrevious(),
        items = data
    )

    constructor(data: Collection<*>, pageable: Pageable, total: Long) : this(
        current = pageable.pageNumber + 1,
        size = pageable.pageSize,
        totalItems = total,
        totalPages = if (pageable.pageSize == 0) 0 else ((total + pageable.pageSize - 1) / pageable.pageSize).toInt(),
        hasNext = (pageable.pageNumber + 1) * pageable.pageSize < total,
        hasPrevious = pageable.pageNumber > 0,
        items = data
    )

    fun build(): ResponseEntity<PageResponse> {
        return ResponseEntity(this, OK)
    }

    /**
     * Retrieves the ObjectMapper bean from the application context.
     * @return The ObjectMapper bean.
     */
    private fun mapper(): ObjectMapper {
        return AppContextUtil.bean(ObjectMapper::class.java)
    }

    companion object {
        /**
         * Constructs a new PageResponse from a Page and a list of data.
         * @param page The Page to construct the PageResponse from.
         * @param data The data to include in the PageResponse.
         * @return The constructed PageResponse.
         */
        fun of(page: Page<*>, data: Collection<*>): ResponseEntity<PageResponse> {
            return PageResponse(
                current = page.number + 1,
                size = page.size,
                totalItems = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                items = data
            ).build()
        }

        /**
         * Constructs a new PageResponse from a collection of data, a Pageable, and the total number of items.
         * @param data The data to include in the PageResponse.
         * @param pageable The Pageable to construct the PageResponse from.
         * @param total The total number of items.
         * @return The constructed PageResponse.
         */
        fun of(data: Collection<*>, pageable: Pageable, total: Long): ResponseEntity<PageResponse> {
            return PageResponse(
                current = pageable.pageNumber + 1,
                size = pageable.pageSize,
                totalItems = total,
                totalPages = if (pageable.pageSize == 0) 0 else ((total + pageable.pageSize - 1) / pageable.pageSize).toInt(),
                hasNext = (pageable.pageNumber + 1) * pageable.pageSize < total,
                hasPrevious = pageable.pageNumber > 0,
                items = data
            ).build()
        }

        /**
         * Constructs an empty PageResponse.
         * @return The constructed empty PageResponse.
         */
        fun empty(): ResponseEntity<PageResponse> {
            return PageResponse(
                current = 1,
                size = 0,
                totalItems = 0,
                totalPages = 0,
                hasNext = false,
                hasPrevious = false,
                items = emptyList<Any>()
            ).build()
        }
    }
}