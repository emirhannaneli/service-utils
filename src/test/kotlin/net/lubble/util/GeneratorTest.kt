package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GeneratorTest {

    @Test
    fun `code is six digits`() {
        repeat(200) { assertThat(Generator.code()).matches("^[0-9]{6}$") }
    }

    @Test
    fun `codes are not repeated in a way a weak generator would`() {
        val codes = (1..2_000).map { Generator.code() }.toSet()

        // 2000 draws out of 1_000_000: a handful of collisions at most.
        assertThat(codes.size).isGreaterThan(1_950)
    }

    @Test
    fun `password uses only lowercase letters by default`() {
        val password = Generator.password(numbers = false, upper = false, special = false, length = 64)

        assertThat(password).hasSize(64).matches("^[a-z]+$")
    }

    @Test
    fun `password draws from every enabled character class`() {
        val password = Generator.password(numbers = true, upper = true, special = true, length = 2_000)

        assertThat(password).hasSize(2_000)
        assertThat(password).containsPattern("[a-z]").containsPattern("[A-Z]").containsPattern("[0-9]")
        assertThat(password.any { it in "!@#$%^&*_=+-/.?" }).isTrue()
        assertThat(password).matches("^[a-zA-Z0-9!@#$%^&*_=+\\-/.?]+$")
    }

    @Test
    fun `password of length zero is empty`() {
        assertThat(Generator.password(numbers = true, upper = true, special = true, length = 0)).isEmpty()
    }
}
