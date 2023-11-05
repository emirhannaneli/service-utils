package net.lubble.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.ResponseEntity
import java.util.*

data class Response(
    @JsonProperty("message")
    val message: String,
    @JsonProperty("status")
    val status: HttpStatus = OK,
    @JsonProperty("code")
    val code: String? = null,
    @JsonProperty("details")
    val details: Any? = null
) {
    fun build(): ResponseEntity<Response> {
        return ResponseEntity(this, status)
    }
    companion object {
        @Suppress("unused")
        fun ofPage(key: String, message: String, page: Page<*>, data: List<*>): Response {
            return Response(
                message = source().getMessage(message, null, locale()),
                details = mapOf(
                    "current" to page.number,
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
    }
}