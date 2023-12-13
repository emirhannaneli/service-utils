package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import java.util.*

@JsonPropertyOrder("message", "status", "code", "details")
data class Response(
    @JsonProperty("message")
    var message: String,
    @JsonProperty("status")
    val status: HttpStatus = OK,
    @JsonProperty("code")
    val code: String? = null,
    @JsonProperty("details")
    val details: Any? = null
) {
    fun build(): ResponseEntity<Response> {
        this.apply {
            message = source().getMessage(message, null, locale())
        }
        return ResponseEntity(this, status)
    }

    fun servletHandler(response: HttpServletResponse) {
        this.apply { message = source().getMessage(message, null, locale()) }
        response.status = status.value()
        response.writer.write(mapper().writeValueAsString(this))
        response.characterEncoding = "UTF-8"
        response.contentType = "application/json;charset=UTF-8"
        response.writer.flush()
    }

    companion object {
        @Suppress("unused")
        fun ofPage(page: Page<*>, data: List<*>): PageResponse {
            return PageResponse(
                current = page.number + 1,
                size = page.size,
                totalItems = page.totalElements,
                totalPages = page.totalPages,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                items = data
            )
        }

        private fun source(): MessageSource {
            return AppContextUtil.bean(MessageSource::class.java)
        }

        private fun locale(): Locale {
            return LocaleContextHolder.getLocale()
        }

        private fun mapper(): ObjectMapper {
            return AppContextUtil.bean(ObjectMapper::class.java)
        }
    }
}

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
    val items: List<*>
) {
    fun build(): ResponseEntity<PageResponse> {
        return ResponseEntity(this, OK)
    }
}