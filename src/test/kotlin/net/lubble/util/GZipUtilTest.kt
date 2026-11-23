package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.zip.ZipException

class GZipUtilTest {

    @Test
    fun `round trips text including non ascii characters`() {
        val text = "Çağrı Şükrü — ünïcödé ✓ ".repeat(50)

        assertThat(GZipUtil.decompress(GZipUtil.compress(text))).isEqualTo(text)
    }

    @Test
    fun `round trips the empty string`() {
        assertThat(GZipUtil.decompress(GZipUtil.compress(""))).isEmpty()
    }

    @Test
    fun `compresses repetitive input`() {
        val text = "a".repeat(10_000)

        assertThat(GZipUtil.compress(text).size).isLessThan(200)
    }

    @Test
    fun `refuses to inflate past the limit`() {
        val bomb = GZipUtil.compress("a".repeat(100_000))

        assertThatThrownBy { GZipUtil.decompress(bomb, 1_000) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("1000")
    }

    @Test
    fun `accepts data exactly at the limit`() {
        val data = GZipUtil.compress("a".repeat(1_000))

        assertThat(GZipUtil.decompress(data, 1_000)).hasSize(1_000)
    }

    @Test
    fun `rejects input that is not gzip`() {
        assertThatThrownBy { GZipUtil.decompress("plain".toByteArray()) }.isInstanceOf(ZipException::class.java)
    }
}
