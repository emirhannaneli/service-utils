package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LKTest {

    private val expectedFormat = Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$")

    @Test
    fun `default constructor produces 17 character three-block lowercase alphanumeric value`() {
        val lk = LK()

        assertThat(lk.value).hasSize(17)
        assertThat(lk.value).matches(expectedFormat.toPattern())
    }

    @Test
    fun `default constructor produces unique values across many invocations`() {
        val values = (1..10_000).map { LK().value }.toSet()

        assertThat(values).hasSize(10_000)
    }

    @Test
    fun `string constructor preserves arbitrary input including legacy 11-char format`() {
        val legacy = "abcde-12345"

        val lk = LK(legacy)

        assertThat(lk.value).isEqualTo(legacy)
    }
}
