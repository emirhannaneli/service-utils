package net.lubble.util

import jakarta.servlet.http.HttpServletRequest
import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
open class RequestTool(private val request: HttpServletRequest) {
    private val logger = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
    init {
        logger.info("Lubble Utils RequestTool initialized.")
    }
    fun hasHeader(header: String): Boolean {
        return request.headerNames.toList().contains(header)
    }

    fun header(header: String): String? {
        return request.getHeader(header)
    }

    fun headers(): List<String> {
        return request.headerNames.toList()
    }

    fun method(): String {
        return request.method
    }

    fun path(): String {
        return request.requestURI
    }

    fun query(): String? {
        return request.queryString
    }

    fun url(): String {
        return request.requestURL.toString()
    }

    fun ip(): String {
        return request.remoteAddr
    }

    fun port(): Int {
        return request.remotePort
    }

    fun protocol(): String {
        return request.protocol
    }

    fun scheme(): String {
        return request.scheme
    }

    fun host(): String {
        return request.remoteHost
    }

    fun userAgent(): String {
        return request.getHeader("User-Agent")
    }

    fun referer(): String {
        return request.getHeader("Referer")
    }

    fun accept(): String {
        return request.getHeader("Accept")
    }

    fun acceptLanguage(): String {
        return request.getHeader("Accept-Language")
    }

    fun acceptEncoding(): String {
        return request.getHeader("Accept-Encoding")
    }

    fun acceptCharset(): String {
        return request.getHeader("Accept-Charset")
    }

    fun cookie(name: String): String? {
        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == name) {
                    return cookie.value
                }
            }
        }
        return null
    }

    fun cookies(): List<String> {
        val cookies = request.cookies
        val list = mutableListOf<String>()
        if (cookies != null) {
            for (cookie in cookies) {
                list.add(cookie.name)
            }
        }
        return list
    }

    fun param(name: String): String? {
        return request.getParameter(name)
    }

    fun params(): List<String> {
        return request.parameterMap.keys.toList()
    }

    fun body(): String {
        return request.reader.readText()
    }
}