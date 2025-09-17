package net.lubble.util.reactive

import net.lubble.util.GZipUtil
import net.lubble.util.TestApplicationContext
import net.lubble.util.enum.CookieType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Base64

class ReactiveCookieUtilTest {
    init {
        TestApplicationContext
        TestApplicationContext.updateCookiePrefix("svc-")
    }

    @Test
    fun `builder encodes gzip values`() {
        val cookie = ReactiveCookieUtil()
            .builder()
            .name("svc-auth")
            .value("payload")
            .maxAge(Duration.ofMinutes(5))
            .secure(true)
            .httpOnly(true)
            .gzip(true)
            .build()

        val decoded = GZipUtil.decompress(Base64.getDecoder().decode(cookie.value))

        assertEquals("payload", decoded)
        assertEquals(Duration.ofMinutes(5), cookie.maxAge)
        assertEquals("Strict", cookie.sameSite)
    }

    @Test
    fun `cookie type values reflect configured prefix`() {
        assertEquals("svc-auth", CookieType.AUTHENTICATION.value)
        assertEquals("svc-refresh", CookieType.REFRESH.value)
        assertEquals("svc-profile", CookieType.PROFILE.value)
    }
}
