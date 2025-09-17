package net.lubble.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GZipUtilTest {
    init {
        TestApplicationContext
    }

    @Test
    fun `compress and decompress round trip`() {
        val payload = "serde-with-üñîçødê"

        val compressed = GZipUtil.compress(payload)
        val restored = GZipUtil.decompress(compressed)

        assertEquals(payload, restored)
    }
}
