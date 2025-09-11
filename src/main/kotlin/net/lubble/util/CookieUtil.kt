package net.lubble.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.enum.CookieType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component
import java.util.*

/**
 * CookieUtil is a utility class that provides methods for handling cookies.
 * It is initialized with an instance of HttpServletResponse.
 *
 * @property response The HttpServletResponse instance this utility class operates on.
 * @property log The logger instance used for logging.
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class CookieUtil(val response: HttpServletResponse) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    var domain: String? = null
    var sameSite: String = "None"

    /**
     * Logs a message indicating that the CookieUtil has been initialized.
     */
    init {
        log.info("Lubble Utils CookieUtil initialized.")
    }

    /**
     * Creates a new cookie with the given parameters, adds it to the response, and returns it.
     *
     * @param name The name of the cookie.
     * @param value The value of the cookie.
     * @param maxAge The maximum age of the cookie in seconds.
     * @param path The path of the cookie.
     * @param secure Whether the cookie is secure.
     * @param httpOnly Whether the cookie is HTTP only.
     * @param gzip Whether the cookie value should be gzipped.
     * @return The created cookie.
     */
    fun create(name: String, value: String, maxAge: Int, path: String, secure: Boolean, httpOnly: Boolean, gzip: Boolean): Cookie {
        val encoded: String = if (gzip) Base64.getEncoder().encodeToString(GZipUtil.compress(value))
        else Base64.getEncoder().encodeToString(value.toByteArray())

        val cookie = Cookie(name, encoded)
        cookie.maxAge = maxAge
        cookie.path = path
        cookie.secure = secure
        cookie.isHttpOnly = httpOnly
        domain?.let { cookie.domain = it }
        cookie.setAttribute("SameSite", sameSite)
        cookie.setAttribute("Type", if (gzip) "gzip" else "plain")
        response.addCookie(cookie)
        return cookie
    }

    /**
     * Clears the cookies with the given names by setting their max age to 0 and adding them to the response.
     *
     * @param cookies The names of the cookies to clear.
     */
    fun clear(cookies: Array<String>) {
        val cleared = cookies.map { Cookie(it, null) }
        cleared.forEach { cookie ->
            cookie.maxAge = 0
            cookie.path = "/"
            cookie.isHttpOnly = true
            domain?.let { cookie.domain = it }
            cookie.setAttribute("SameSite", sameSite)
            response.addCookie(cookie)
        }
    }

    /**
     * Clears all cookies by setting their max age to 0 and adding them to the response.
     */
    fun clearAll() {
        val cookies = CookieType.entries
        val cleared = cookies.map { Cookie(it.value, null) }
        cleared.forEach {
            it.maxAge = 0
            it.path = "/"
            it.isHttpOnly = true
            it.setAttribute("SameSite", sameSite)
            response.addCookie(it)
        }
    }
}