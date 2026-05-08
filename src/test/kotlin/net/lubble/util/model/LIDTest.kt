package net.lubble.util.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LIDTest {

    companion object {
        init {
            // Force LK class load to register BouncyCastle provider
            Class.forName("net.lubble.util.LK")
        }
    }

    private val pkMax: Long = (1L shl 53) - 1   // 9_007_199_254_740_991

    @Test
    fun `newPk returns values within zero to two pow fifty three minus one inclusive`() {
        repeat(10_000) {
            val pk = LID.newPk()

            assertThat(pk).isBetween(0L, pkMax)
        }
    }

    @Test
    fun `newPk produces no collisions across one hundred thousand invocations`() {
        val pks = (1..100_000).map { LID.newPk() }.toSet()

        assertThat(pks).hasSize(100_000)
    }

    @Test
    fun `newPk never returns negative even at extreme distribution edges`() {
        // SecureRandom.nextLong() can produce Long.MIN_VALUE; mask must still keep result non-negative.
        repeat(100_000) {
            assertThat(LID.newPk()).isNotNegative()
        }
    }

    @Test
    fun `default LID constructor uses newPk masked range`() {
        repeat(1_000) {
            val lid = LID()

            assertThat(lid.pk).isBetween(0L, pkMax)
            assertThat(lid.sk.value).matches(Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$").toPattern())
            assertThat(lid.getId()).hasSize(26)
        }
    }

    @Test
    fun `default BaseModel constructor delegates pk to LID newPk`() {
        repeat(1_000) {
            val model = BaseModel()

            assertThat(model.pk).isBetween(0L, pkMax)
            assertThat(model.sk.value).matches(Regex("^[a-z0-9]{5}-[a-z0-9]{5}-[a-z0-9]{5}$").toPattern())
        }
    }
}
