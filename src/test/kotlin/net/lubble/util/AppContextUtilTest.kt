package net.lubble.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AppContextUtilTest {
    init {
        TestApplicationContext
    }

    @Test
    fun `retrieves bean by type`() {
        val mapper = AppContextUtil.bean(ObjectMapper::class.java)

        assertNotNull(mapper)
    }

    @Test
    fun `retrieves bean by name`() {
        val greeting: String = AppContextUtil.bean("greetingBean")

        assertEquals("hello-world", greeting)
    }

    @Test
    fun `throws descriptive error when bean missing`() {
        val ex = assertThrows(RuntimeException::class.java) {
            AppContextUtil.bean<Any>("missingBean")
        }

        assertEquals("Could not get bean (missingBean)", ex.message)
    }
}
