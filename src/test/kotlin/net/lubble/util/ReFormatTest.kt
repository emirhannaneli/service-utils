package net.lubble.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class ReFormatTest {
    init {
        TestApplicationContext
    }

    @Test
    fun `chains operations with default locale`() {
        val formatted = ReFormat("  merhaba dünya  ")
            .trim()
            .capitalize()
            .format()

        assertEquals("Merhaba dünya", formatted)
    }

    @Test
    fun `uses provided locale for casing`() {
        val formatted = ReFormat("istanbul")
            .upper(Locale("tr", "TR"))
            .format()

        assertEquals("İSTANBUL", formatted)
    }
}
