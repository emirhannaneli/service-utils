package net.lubble.util

import net.lubble.util.exception.NotFoundException
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

class ResponseTest {
    companion object {
        private val mapper: ObjectMapper = JsonMapper.builderWithJackson2Defaults().build()

        @JvmStatic
        @BeforeAll
        fun context() {
            TestContext.install(
                mapOf(
                    "item.saved" to "Kayıt başarılı",
                    "global.exception.not.found" to "{0} bulunamadı ({1})",
                )
            ) { it.beanFactory.registerSingleton("objectMapper", mapper) }
        }

        @JvmStatic
        @AfterAll
        fun reset() = TestContext.reset()
    }

    private fun json(value: Any) = mapper.readTree(mapper.writeValueAsString(value))

    @Test
    fun `message is localized and defaults to OK without code or details`() {
        val entity = Response("item.saved").build()

        assertThat(entity.statusCode).isEqualTo(HttpStatus.OK)
        val body = json(entity.body!!)
        assertThat(body["message"].asString()).isEqualTo("Kayıt başarılı")
        assertThat(body["status"].asString()).isEqualTo("OK")
        assertThat(body["code"].isNull).isTrue()
        assertThat(body["details"].isNull).isTrue()
        assertThat(body.propertyNames().toList()).containsExactly("message", "status", "code", "details")
    }

    @Test
    fun `unknown message codes are used as plain text`() {
        val body = json(Response("Plain text, not a code", HttpStatus.ACCEPTED, mapOf("k" to "v")).build().body!!)

        assertThat(body["message"].asString()).isEqualTo("Plain text, not a code")
        assertThat(body["status"].asString()).isEqualTo("ACCEPTED")
        assertThat(body["details"]["k"].asString()).isEqualTo("v")
    }

    @Test
    fun `every constructor carries its arguments`() {
        assertThat(Response("m", HttpStatus.CREATED).build().statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(json(Response("m", "details"))["details"].asString()).isEqualTo("details")
        val full = json(Response("m", HttpStatus.BAD_REQUEST, "0x1", listOf(1, 2)))
        assertThat(full["code"].asString()).isEqualTo("0x1")
        assertThat(full["details"].size()).isEqualTo(2)
    }

    @Test
    fun `is built from an exception model`() {
        val exception = NotFoundException("User", 42)

        val entity = Response(exception, exception.details()).build()

        assertThat(entity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        val body = json(entity.body!!)
        assertThat(body["message"].asString()).isEqualTo("User bulunamadı (42)")
        assertThat(body["code"].asString()).isEqualTo("0x0LB404")
        assertThat(body["details"]["name"].asString()).isEqualTo("User")
        assertThat(json(Response(exception))["details"].isNull).isTrue()
    }

    @Test
    fun `servlet handler writes utf8 json with the response status`() {
        val servletResponse = MockHttpServletResponse()

        Response("item.saved", HttpStatus.CONFLICT).servletHandler(servletResponse)

        assertThat(servletResponse.status).isEqualTo(409)
        assertThat(servletResponse.contentType).isEqualTo("application/json;charset=UTF-8")
        assertThat(servletResponse.characterEncoding).isEqualTo("UTF-8")
        // Read the raw bytes as UTF-8: the encoding must have been applied before writing.
        assertThat(String(servletResponse.contentAsByteArray, Charsets.UTF_8)).contains("Kayıt başarılı")
    }

    @Test
    fun `page response mirrors a spring page`() {
        val page = PageImpl(listOf("c", "d"), PageRequest.of(1, 2), 5)

        val response = PageResponse.of(page, listOf("C", "D")).body!!

        assertThat(response).isEqualTo(PageResponse(page, listOf("C", "D")))
        assertThat(response.current).isEqualTo(2)
        assertThat(response.size).isEqualTo(2)
        assertThat(response.totalItems).isEqualTo(5)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.hasNext).isTrue()
        assertThat(response.hasPrevious).isTrue()
        assertThat(response.items).containsExactly("C", "D")
        assertThat(Response.of(page, listOf("C", "D")).body).isEqualTo(response)
    }

    @Test
    fun `page response is computed from pageable and total`() {
        val first = PageResponse.of(listOf(1, 2, 3), PageRequest.of(0, 3), 7).body!!
        assertThat(first.current).isEqualTo(1)
        assertThat(first.totalPages).isEqualTo(3)
        assertThat(first.hasNext).isTrue()
        assertThat(first.hasPrevious).isFalse()

        val last = PageResponse(listOf(7), PageRequest.of(2, 3), 7)
        assertThat(last.hasNext).isFalse()
        assertThat(last.hasPrevious).isTrue()
        assertThat(Response.of(listOf(7), PageRequest.of(2, 3), 7).body).isEqualTo(last)

        val exact = PageResponse(listOf(4, 5, 6), PageRequest.of(1, 3), 6)
        assertThat(exact.totalPages).isEqualTo(2)
        assertThat(exact.hasNext).isFalse()
    }

    @Test
    fun `hasNext does not overflow for huge page sizes`() {
        val response = PageResponse(emptyList<Any>(), PageRequest.of(3, Int.MAX_VALUE), 10)

        assertThat(response.hasNext).isFalse()
    }

    @Test
    fun `empty page response`() {
        val empty = PageResponse.empty().body!!

        assertThat(empty.current).isEqualTo(1)
        assertThat(empty.totalItems).isZero()
        assertThat(empty.items).isEmpty()
        assertThat(json(empty).propertyNames().toList())
            .containsExactly("current", "size", "totalItems", "totalPages", "hasNext", "hasPrevious", "items")
    }

    @Test
    fun `graph response localizes messages and carries exception data`() {
        assertThat(GraphResponse("item.saved").message).isEqualTo("Kayıt başarılı")
        assertThat(GraphResponse(message = "item.saved", code = "0x1").code).isEqualTo("0x1")
        assertThat(GraphResponse("item.saved", mapOf("a" to 1)).details).isEqualTo(mapOf("a" to 1))

        val exception = NotFoundException("User", 42)
        val fromException = GraphResponse(exception, exception.details())
        assertThat(fromException.message).isEqualTo("User bulunamadı (42)")
        assertThat(fromException.code).isEqualTo("0x0LB404")
        assertThat(GraphResponse(exception).details).isNull()
        assertThat(json(GraphResponse("m")).propertyNames().toList()).containsExactly("message", "code", "details")
    }

    @Test
    fun `graph page response exposes meta and data`() {
        val page = PageImpl(listOf("c", "d"), PageRequest.of(1, 2), 5)

        val fromPage = GraphResponse.of(page, listOf("C", "D"))
        assertThat(fromPage.data).containsExactly("C", "D")
        assertThat(fromPage.meta.current).isEqualTo(2)
        assertThat(fromPage.meta.totalPages).isEqualTo(3)
        assertThat(fromPage.meta.hasNext).isTrue()
        assertThat(fromPage.meta.hasPrevious).isTrue()

        val computed = GraphPageResponse.of(listOf(1), PageRequest.of(0, 1), 1)
        assertThat(computed.meta.totalItems).isEqualTo(1)
        assertThat(computed.meta.hasNext).isFalse()
        assertThat(GraphResponse.of(listOf(1), PageRequest.of(0, 1), 1).meta.totalPages).isEqualTo(1)
        assertThat(GraphPageResponse.of(page, listOf("C")).meta.size).isEqualTo(2)

        assertThat(GraphPageResponse.empty().data).isEmpty()
        assertThat(json(fromPage).propertyNames().toList()).containsExactly("meta", "data")
    }
}
