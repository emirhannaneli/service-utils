package net.lubble.util

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class RequestToolTest {
    init {
        TestApplicationContext
    }

    @Test
    fun `exposes request metadata and headers`() {
        val request = MockHttpServletRequest().apply {
            method = "POST"
            scheme = "https"
            serverName = "api.lubble.dev"
            serverPort = 8443
            requestURI = "/v1/items"
            queryString = "page=1"
            remoteHost = "client.lubble.dev"
            remoteAddr = "10.0.0.5"
            remotePort = 51234
            addHeader("X-Trace-Id", "trace-123")
            addHeader("User-Agent", "JUnit")
            addHeader("Accept-Language", "tr-TR")
            setContent("payload".toByteArray())
            setCookies(Cookie("session", "abc"))
        }

        val tool = RequestTool(request)

        assertTrue(tool.hasHeader("X-Trace-Id"))
        assertEquals("trace-123", tool.header("X-Trace-Id"))
        assertTrue(tool.headers().containsAll(listOf("X-Trace-Id", "User-Agent", "Accept-Language")))
        assertEquals("POST", tool.method())
        assertEquals("/v1/items", tool.path())
        assertEquals("page=1", tool.query())
        assertEquals("https://api.lubble.dev:8443/v1/items", tool.url())
        assertEquals("10.0.0.5", tool.ip())
        assertEquals(51234, tool.port())
        assertEquals("HTTP/1.1", tool.protocol())
        assertEquals("https", tool.scheme())
        assertEquals("client.lubble.dev", tool.host())
        assertEquals("JUnit", tool.userAgent())
        assertEquals("tr-TR", tool.acceptLanguage())
        assertEquals("abc", tool.cookie("session"))
        assertTrue(tool.cookies().contains("session"))
        assertEquals("payload", tool.body())
    }
}
