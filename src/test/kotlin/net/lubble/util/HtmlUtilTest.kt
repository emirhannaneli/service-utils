package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HtmlUtilTest {

    @Test
    fun `returns the input when the text is short enough`() {
        val html = "<p>Hello <b>world</b></p>"

        assertThat(HtmlUtil.shorten(html, 50)).isSameAs(html)
    }

    @Test
    fun `shortens plain text and appends an ellipsis`() {
        assertThat(HtmlUtil.shorten("abcdefghijklmnopqrstuvwxyz", 10)).isEqualTo("abcdefghij...")
    }

    @Test
    fun `closes tags that were left open by the cut`() {
        val shortened = HtmlUtil.shorten("<p>abcdefghij <b>klmnopqrstuvwxyz</b> more text here</p>", 20)

        assertThat(shortened).startsWith("<p>abcdefghij <b>").endsWith("...")
        assertThat(shortened).contains("</b>").contains("</p>")
        assertThat(shortened.indexOf("</b>")).isLessThan(shortened.indexOf("</p>"))
    }

    @Test
    fun `does not close void tags`() {
        val shortened = HtmlUtil.shorten("<p>abc<br>defghijklmnopqrstuvwxyz and more</p>", 15)

        assertThat(shortened).doesNotContain("</br>").contains("</p>")
    }

    @Test
    fun `does not fail when a tag alone is longer than the limit`() {
        val html = "<a href=\"https://example.com/a/very/long/link/that/exceeds/the/limit\">some link text that is long</a>"

        val shortened = HtmlUtil.shorten(html, 5)

        assertThat(shortened).startsWith("<a href=").contains("</a>")
    }
}
