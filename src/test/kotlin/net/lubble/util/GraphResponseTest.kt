package net.lubble.util

import net.lubble.util.model.ExceptionModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus

class GraphResponseTest {

    private val exceptionModel = object : ExceptionModel {
        override fun message(): String = "Translated message"
        override fun status(): HttpStatus = HttpStatus.BAD_REQUEST
        override fun code(): String = "0xTEST"
    }

    @Test
    fun `constructing from exception model keeps translated message`() {
        val response = GraphResponse(exceptionModel)

        assertEquals("Translated message", response.message)
        assertEquals("0xTEST", response.code)
        assertNull(response.details)
    }

    @Test
    fun `constructing from exception model with details keeps translated message`() {
        val details = mapOf("detail" to "value")

        val response = GraphResponse(exceptionModel, details)

        assertEquals("Translated message", response.message)
        assertEquals("0xTEST", response.code)
        assertEquals(details, response.details)
    }

    @Test
    fun `graph page response exposes pagination metadata`() {
        val items = listOf("item-1", "item-2")
        val pageable = PageRequest.of(1, 2)
        val page = PageImpl(items, pageable, 6)

        val response = GraphResponse.of(page, items)

        assertEquals(2, response.meta.current)
        assertEquals(2, response.meta.size)
        assertEquals(6, response.meta.totalItems)
        assertEquals(3, response.meta.totalPages)
        assertEquals(true, response.meta.hasNext)
        assertEquals(true, response.meta.hasPrevious)
        assertEquals(items, response.data)
    }
}
