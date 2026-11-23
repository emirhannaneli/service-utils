package net.lubble.util

import net.lubble.util.config.lubble.LubbleConfig
import net.lubble.util.reactive.ReactiveCookieUtil
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpResponse
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Duration
import java.util.Base64

class CookieUtilTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun context() {
            TestContext.install {
                it.beanFactory.registerSingleton("lubbleConfig", LubbleConfig().apply { security.cookie.prefix = "lb_" })
            }
        }

        @JvmStatic
        @AfterAll
        fun reset() = TestContext.reset()
    }

    private val response = MockHttpServletResponse()
    private val cookies = CookieUtil(response)

    @Test
    fun `creates a base64 encoded cookie with the requested attributes`() {
        cookies.domain = "example.com"
        cookies.sameSite = "Lax"

        val cookie = cookies.create("session", "héllo", 3600, "/app", secure = true, httpOnly = true, gzip = false)

        assertThat(String(Base64.getDecoder().decode(cookie.value))).isEqualTo("héllo")
        assertThat(cookie.maxAge).isEqualTo(3600)
        assertThat(cookie.path).isEqualTo("/app")
        assertThat(cookie.secure).isTrue()
        assertThat(cookie.isHttpOnly).isTrue()
        assertThat(cookie.domain).isEqualTo("example.com")
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax")
        assertThat(cookie.getAttribute("Type")).isEqualTo("plain")
        assertThat(response.getCookie("session")).isSameAs(cookie)
    }

    @Test
    fun `gzipped cookie value can be decompressed again`() {
        val cookie = cookies.create("cart", "x".repeat(500), 60, "/", secure = false, httpOnly = false, gzip = true)

        assertThat(cookie.getAttribute("Type")).isEqualTo("gzip")
        assertThat(cookie.value.length).isLessThan(500)
        assertThat(GZipUtil.decompress(Base64.getDecoder().decode(cookie.value))).isEqualTo("x".repeat(500))
    }

    @Test
    fun `clear expires the named cookies on the configured domain`() {
        cookies.domain = "example.com"

        cookies.clear(arrayOf("a", "b"))

        assertThat(response.cookies.map { it.name }).containsExactly("a", "b")
        response.cookies.forEach {
            assertThat(it.maxAge).isZero()
            assertThat(it.path).isEqualTo("/")
            assertThat(it.domain).isEqualTo("example.com")
            assertThat(it.isHttpOnly).isTrue()
        }
    }

    @Test
    fun `clearing SameSite None cookies marks them secure so browsers accept the removal`() {
        cookies.clear(arrayOf("a"))

        assertThat(response.getCookie("a")!!.getAttribute("SameSite")).isEqualTo("None")
        assertThat(response.getCookie("a")!!.secure).isTrue()
    }

    @Test
    fun `clearAll expires every known cookie type with prefix and domain`() {
        cookies.domain = "example.com"
        cookies.sameSite = "Strict"

        cookies.clearAll()

        assertThat(response.cookies.map { it.name })
            .containsExactly("lb_auth", "lb_refresh", "lb_profile", "lb_cart", "lb_wishlist")
        response.cookies.forEach {
            assertThat(it.maxAge).isZero()
            assertThat(it.domain).isEqualTo("example.com")
            assertThat(it.secure).isFalse()
        }
    }

    @Test
    fun `reactive builder creates an encoded strict cookie`() {
        val cookie = ReactiveCookieUtil().builder()
            .name("session").value("hello").maxAge(Duration.ofMinutes(5)).path("/app").secure(true).httpOnly(true)
            .build()

        assertThat(String(Base64.getDecoder().decode(cookie.value))).isEqualTo("hello")
        assertThat(cookie.maxAge).isEqualTo(Duration.ofMinutes(5))
        assertThat(cookie.path).isEqualTo("/app")
        assertThat(cookie.isSecure).isTrue()
        assertThat(cookie.isHttpOnly).isTrue()
        assertThat(cookie.sameSite).isEqualTo("Strict")
    }

    @Test
    fun `reactive builder can gzip the value`() {
        val cookie = ReactiveCookieUtil().builder().name("cart").value("y".repeat(300)).gzip(true).build()

        assertThat(GZipUtil.decompress(Base64.getDecoder().decode(cookie.value))).isEqualTo("y".repeat(300))
    }

    @Test
    fun `reactive clear and clearAll expire cookies`() {
        val reactive = MockServerHttpResponse()

        ReactiveCookieUtil().clear(arrayOf("a"), reactive)
        ReactiveCookieUtil().clearAll(reactive)

        assertThat(reactive.cookies.keys).containsExactlyInAnyOrder("a", "lb_auth", "lb_refresh", "lb_profile", "lb_cart", "lb_wishlist")
        reactive.cookies.values.flatten().forEach { assertThat(it.maxAge).isEqualTo(Duration.ZERO) }
    }
}
