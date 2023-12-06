package net.lubble.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnClass(Cookie::class)
class CookieUtil(private var response: HttpServletResponse) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    init {
        log.info("Lubble Utils CookieUtil initialized.")
    }
    fun create(name: String, value: String, maxAge: Int, path: String, secure: Boolean, httpOnly: Boolean, gzip: Boolean): Cookie {
        val encoded: String = if (gzip) Base64.getEncoder().encodeToString(GZipUtil.compress(value))
        else Base64.getEncoder().encodeToString(value.toByteArray())

        val cookie = Cookie(name, encoded)
        cookie.maxAge = maxAge
        cookie.path = path
        cookie.secure = secure
        cookie.isHttpOnly = httpOnly
        cookie.setAttribute("SameSite", "Strict")
        cookie.setAttribute("Type", if (gzip) "gzip" else "plain")
        response.addCookie(cookie)
        return cookie
    }

    fun clear(cookies: Array<String>) {
        val cleared = cookies.map { Cookie(it, null) }
        cleared.forEach {
            it.maxAge = 0
            it.path = "/"
            it.isHttpOnly = true
            it.setAttribute("SameSite", "Strict")
            response.addCookie(it)
        }
    }
}