package net.lubble.util

import net.lubble.util.model.ExceptionModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus

class ResponseTest {

    private val exceptionModel = object : ExceptionModel {
        override fun message(): String = "Another translated message"
        override fun status(): HttpStatus = HttpStatus.I_AM_A_TEAPOT
        override fun code(): String = "0xC0DE"
    }

    @Test
    fun `response entity retains status from exception model`() {
        val response = Response(exceptionModel)

        val entity = response.build()

        assertEquals(HttpStatus.I_AM_A_TEAPOT, entity.statusCode)
        assertEquals("Another translated message", entity.body!!.extractMessage())
        assertEquals("0xC0DE", entity.body!!.extractCode())
    }

    @Test
    fun `page response exposes pagination details`() {
        val items = listOf("alpha", "beta")
        val pageable = PageRequest.of(0, 2)
        val page = PageImpl(items, pageable, 4)

        val responseEntity = Response.of(page, items)
        val body = responseEntity.body!!

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        assertEquals(1, body.current)
        assertEquals(2, body.size)
        assertEquals(4, body.totalItems)
        assertEquals(2, body.totalPages)
        assertEquals(false, body.hasPrevious)
        assertEquals(true, body.hasNext)
        assertEquals(items, body.items)
    }

    private fun Response.extractMessage(): String {
        val field = Response::class.java.getDeclaredField("message")
        field.isAccessible = true
        return field.get(this) as String
    }

    private fun Response.extractCode(): String? {
        val field = Response::class.java.getDeclaredField("code")
        field.isAccessible = true
        return field.get(this) as String?
    }
}
