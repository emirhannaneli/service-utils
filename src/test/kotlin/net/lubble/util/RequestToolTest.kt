package net.lubble.util

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class RequestToolTest {
    private val request = MockHttpServletRequest("POST", "/api/items").apply {
        queryString = "q=1"
        addHeader("User-Agent", "junit")
        addHeader("X-Custom", "yes")
        setParameter("q", "1")
        setCookies(Cookie("session", "abc"), Cookie("theme", "dark"))
        setContent("payload".toByteArray())
        remoteAddr = "10.0.0.7"
        remotePort = 4242
        remoteHost = "client.local"
        scheme = "https"
        protocol = "HTTP/1.1"
    }
    private val tool = RequestTool(request)

    @Test
    fun `reads request line details`() {
        assertThat(tool.method()).isEqualTo("POST")
        assertThat(tool.path()).isEqualTo("/api/items")
        assertThat(tool.query()).isEqualTo("q=1")
        assertThat(tool.url()).endsWith("/api/items")
        assertThat(tool.ip()).isEqualTo("10.0.0.7")
        assertThat(tool.port()).isEqualTo(4242)
        assertThat(tool.host()).isEqualTo("client.local")
        assertThat(tool.scheme()).isEqualTo("https")
        assertThat(tool.protocol()).isEqualTo("HTTP/1.1")
    }

    @Test
    fun `header lookup is case insensitive`() {
        assertThat(tool.hasHeader("x-custom")).isTrue()
        assertThat(tool.hasHeader("X-Custom")).isTrue()
        assertThat(tool.hasHeader("X-Missing")).isFalse()
        assertThat(tool.header("x-custom")).isEqualTo("yes")
        assertThat(tool.headers()).contains("User-Agent", "X-Custom")
    }

    @Test
    fun `well known headers are null when absent instead of throwing`() {
        assertThat(tool.userAgent()).isEqualTo("junit")
        assertThat(tool.referer()).isNull()
        assertThat(tool.accept()).isNull()
        assertThat(tool.acceptLanguage()).isNull()
        assertThat(tool.acceptEncoding()).isNull()
        assertThat(tool.acceptCharset()).isNull()
    }

    @Test
    fun `reads cookies params and body`() {
        assertThat(tool.cookie("session")).isEqualTo("abc")
        assertThat(tool.cookie("missing")).isNull()
        assertThat(tool.cookies()).containsExactly("session", "theme")
        assertThat(tool.param("q")).isEqualTo("1")
        assertThat(tool.param("missing")).isNull()
        assertThat(tool.params()).containsExactly("q")
        assertThat(tool.body()).isEqualTo("payload")
    }

    @Test
    fun `request without cookies yields no cookies`() {
        val empty = RequestTool(MockHttpServletRequest())

        assertThat(empty.cookie("session")).isNull()
        assertThat(empty.cookies()).isEmpty()
    }
}
