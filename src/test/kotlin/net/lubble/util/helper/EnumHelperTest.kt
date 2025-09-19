package net.lubble.util.helper

import net.lubble.util.TestApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EnumHelperTest {
    init {
        TestApplicationContext.addMessage("status.active", "Active")
        TestApplicationContext.addMessage("status.passive", "Passive")
    }

    @Test
    fun `find by name returns enum`() {
        val status = EnumHelper.findByName<SampleStatus>("ACTIVE")

        assertSame(SampleStatus.ACTIVE, status)
    }

    @Test
    fun `find by value returns enum`() {
        val status = EnumHelper.findByValue<SampleStatus>("passive")

        assertSame(SampleStatus.PASSIVE, status)
    }

    @Test
    fun `to constant uses localized label`() {
        val constant = SampleStatus.ACTIVE.toConstant()

        assertEquals("Active", constant.label)
        assertEquals("active", constant.value)
        assertEquals("green", constant.color)
        assertEquals("check", constant.icon)
    }

    @Test
    fun `invalid value throws descriptive exception`() {
        val ex = assertThrows(net.lubble.util.exception.InvalidParamException::class.java) {
            EnumHelper.findByValue<SampleStatus>("unknown")
        }

        assertEquals("global.exception.invalid.param", ex.message())
    }
}

enum class SampleStatus(
    override val label: String,
    override val value: String,
    override val color: String?,
    override val icon: String?
) : EnumHelper {
    ACTIVE("status.active", "active", "green", "check"),
    PASSIVE("status.passive", "passive", "red", "close")
}
