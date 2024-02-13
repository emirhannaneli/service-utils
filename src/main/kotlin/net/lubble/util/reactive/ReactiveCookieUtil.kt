package net.lubble.util.reactive

import net.lubble.util.GZipUtil
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.enum.CookieType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.http.ResponseCookie
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*

@Component
@ConditionalOnClass(ResponseCookie::class)
class ReactiveCookieUtil {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    init {
        log.info("Lubble Utils ReactiveCookieUtil initialized.")
    }

    fun builder(): Builder {
        return Builder()
    }

    class Builder {
        private lateinit var name: String
        private lateinit var value: String
        private var maxAge: Duration = Duration.ZERO
        private var path: String = "/"
        private var secure: Boolean = false
        private var httpOnly: Boolean = true
        private var gzip: Boolean = false
        private lateinit var response: ServerHttpResponse

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun value(value: String): Builder {
            this.value = value
            return this
        }

        fun maxAge(maxAge: Duration): Builder {
            this.maxAge = maxAge
            return this
        }

        fun path(path: String): Builder {
            this.path = path
            return this
        }

        fun secure(secure: Boolean): Builder {
            this.secure = secure
            return this
        }

        fun httpOnly(httpOnly: Boolean): Builder {
            this.httpOnly = httpOnly
            return this
        }

        fun gzip(gzip: Boolean): Builder {
            this.gzip = gzip
            return this
        }

        fun response(response: ServerHttpResponse): Builder {
            this.response = response
            return this
        }

        fun build(): ResponseCookie {
            return create(name, value, maxAge, path, secure, httpOnly, gzip)
        }

        private fun create(
            name: String,
            value: String,
            maxAge: Duration,
            path: String,
            secure: Boolean,
            httpOnly: Boolean,
            gzip: Boolean
        ): ResponseCookie {
            val encoded: String = if (gzip) Base64.getEncoder().encodeToString(GZipUtil.compress(value))
            else Base64.getEncoder().encodeToString(value.toByteArray())

            val cookie = ResponseCookie.from(name, encoded)
                .maxAge(maxAge)
                .path(path)
                .secure(secure)
                .httpOnly(httpOnly)
                .sameSite("Strict")
                .build()
            return cookie
        }
    }

    fun clear(cookies: Array<String>, response: ServerHttpResponse) {
        val cleared = cookies.map { ResponseCookie.from(it, "").maxAge(Duration.ZERO).path("/").httpOnly(true).sameSite("Strict").build() }
        cleared.forEach {
            response.addCookie(it)
        }
    }

    fun clearAll(response: ServerHttpResponse) {
        val cookies = CookieType.entries
        val cleared = cookies.map { ResponseCookie.from(it.value, "").maxAge(Duration.ZERO).path("/").httpOnly(true).sameSite("Strict").build() }
        cleared.forEach {
            response.addCookie(it)
        }
    }


}