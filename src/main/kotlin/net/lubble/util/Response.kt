package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import java.nio.charset.StandardCharsets
import java.util.*

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
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = "application/json;charset=UTF-8"
        response.writer.flush()
    }

    companion object {
        @Suppress("unused")
        fun ofPage(key: String, message: String, page: Page<*>, data: List<*>): Response {
            return Response(
                message = source().getMessage(message, null, locale()),
                details = mapOf(
                    "current" to page.number + 1,
                    "size" to page.size,
                    "totalItems" to page.totalElements,
                    "totalPages" to page.totalPages,
                    "hasNext" to page.hasNext(),
                    "hasPrevious" to page.hasPrevious(),
                    key to data
                )
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