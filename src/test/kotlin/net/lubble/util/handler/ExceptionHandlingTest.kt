package net.lubble.util.handler

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import net.lubble.util.Response
import net.lubble.util.config.lubble.LubbleConfig
import net.lubble.util.exception.AccessDenied
import net.lubble.util.exception.AlreadyExistsException
import net.lubble.util.exception.InvalidParamException
import net.lubble.util.exception.NotFoundException
import net.lubble.util.exception.UnAuthorized
import net.lubble.util.exception.WrongCredentials
import net.lubble.util.support.TestContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class ExceptionHandlingTest {
    companion object {
        private val mapper = JsonMapper.builderWithJackson2Defaults().build()

        @JvmStatic
        @BeforeAll
        fun context() {
            TestContext.install(
                mapOf(
                    "global.exception.already.exists" to "{0} already exists ({1})",
                    "global.exception.not.found" to "{0} not found ({1})",
                    "global.exception.internal.error" to "Unexpected error",
                    "global.exception.unsupported.operation" to "Unsupported",
                    "global.exception.invalid.param" to "Invalid parameter",
                    "global.exception.invalid.payload" to "Invalid payload",
                    "global.exception.invalid.method" to "Invalid method",
                    "global.exception.invalid.param.type" to "Invalid parameter type",
                    "global.exception.access.denied" to "Access denied",
                    "global.exception.unauthorized" to "Unauthorized",
                    "global.exception.wrong.credentials" to "Wrong credentials",
                )
            )
        }

        @JvmStatic
        @AfterAll
        fun reset() = TestContext.reset()
    }

    private val base = LubbleRestBaseExceptionHandler()

    private fun body(entity: ResponseEntity<Response>): JsonNode = mapper.readTree(mapper.writeValueAsString(entity.body))

    @Test
    fun `exception models describe themselves`() {
        assertThat(NotFoundException("User", 7).let { Triple(it.message(), it.status(), it.code()) })
            .isEqualTo(Triple("User not found (7)", HttpStatus.NOT_FOUND, "0x0LB404"))
        assertThat(NotFoundException().details()).isEqualTo(mapOf("name" to "UNKNOWN", "value" to "UNKNOWN"))
        assertThat(NotFoundException(null, null).message()).isEqualTo("UNKNOWN not found (UNKNOWN)")

        assertThat(AlreadyExistsException("Email", "a@b.c").let { Triple(it.message(), it.status(), it.code()) })
            .isEqualTo(Triple("Email already exists (a@b.c)", HttpStatus.CONFLICT, "0x0LB409-0"))
        assertThat(AlreadyExistsException("Email", "a@b.c").details()).isEqualTo(mapOf("name" to "Email", "value" to "a@b.c"))

        val invalid = InvalidParamException("age", -1, "positive")
        assertThat(Triple(invalid.message(), invalid.status(), invalid.code()))
            .isEqualTo(Triple("Invalid parameter", HttpStatus.BAD_REQUEST, "0x0LB400-0"))
        assertThat(invalid.details()).isEqualTo(mapOf("param" to "age", "value" to -1, "desired" to "positive"))
        assertThat(InvalidParamException().details()).isEqualTo(mapOf("param" to null, "value" to null, "desired" to null))

        assertThat(AccessDenied("no role").let { listOf(it.message(), it.status(), it.code(), it.details()) })
            .containsExactly("Access denied", HttpStatus.FORBIDDEN, "0x000403-1", "no role")
        assertThat(AccessDenied().details()).isNull()
        assertThat(UnAuthorized("expired").let { listOf(it.message(), it.status(), it.code(), it.details()) })
            .containsExactly("Unauthorized", HttpStatus.UNAUTHORIZED, "0x000401-1", "expired")
        assertThat(UnAuthorized().details()).isNull()
        assertThat(WrongCredentials().let { Triple(it.message(), it.status(), it.code()) })
            .isEqualTo(Triple("Wrong credentials", HttpStatus.UNAUTHORIZED, "0x0LB401-0"))
    }

    @Test
    fun `domain exceptions are translated to their own status and code`() {
        val notFound = base.handleNotFoundException(NotFoundException("User", 7))
        assertThat(notFound.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(body(notFound)["details"]["value"].asInt()).isEqualTo(7)

        val exists = base.handleAlreadyExistsException(AlreadyExistsException("Email", "a@b.c"))
        assertThat(exists.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(body(exists)["code"].asString()).isEqualTo("0x0LB409-0")

        assertThat(base.handleInvalidParamException(InvalidParamException("a", 1, 2)).statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(base.handleWrongCredentials(WrongCredentials()).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(base.handleUnAuthorized(UnAuthorized()).statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)

        val denied = base.handleAccessDenied(AccessDenied())
        assertThat(denied.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(body(denied)["details"].asString()).isEqualTo("unknown")
        assertThat(body(base.handleAccessDenied(AccessDenied("no role")))["details"].asString()).isEqualTo("no role")
    }

    @Test
    fun `unexpected errors do not leak internals by default`() {
        val leaky = IllegalStateException("could not execute statement [SELECT secret FROM users]")

        val response = base.handleException(leaky)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = body(response)
        assertThat(body["message"].asString()).isEqualTo("Unexpected error")
        assertThat(body["code"].asString()).isEqualTo("0x000500-0")
        assertThat(body["details"].isNull).isTrue()
        assertThat(body.toString()).doesNotContain("SELECT").doesNotContain("IllegalStateException")

        assertThat(body(base.handleUnsupportedOperationException(UnsupportedOperationException("internal")))["details"].isNull).isTrue()
        val notImplemented = base.handleNotImplementedError(NotImplementedError("internal"))
        assertThat(notImplemented.statusCode).isEqualTo(HttpStatus.NOT_IMPLEMENTED)
        assertThat(body(notImplemented)["details"].isNull).isTrue()
        assertThat(body(notImplemented)["code"].asString()).isEqualTo("0x000500-2")
    }

    @Test
    fun `unexpected error details are exposed only when explicitly enabled`() {
        val handler = LubbleRestBaseExceptionHandler().apply {
            config = LubbleConfig().apply { exposeExceptionDetails = true }
        }

        val details = body(handler.handleException(IllegalStateException("boom")))["details"]

        assertThat(details["exception"].asString()).isEqualTo("java.lang.IllegalStateException")
        assertThat(details["message"].asString()).isEqualTo("boom")
        assertThat(body(handler.handleUnsupportedOperationException(UnsupportedOperationException("nope")))["details"]["message"].asString())
            .isEqualTo("nope")
    }

    @Test
    fun `web errors are reported as bad requests`() {
        val web = LubbleRestWebExceptionHandler()

        val missing = web.handleMissingServletRequestParameterException(MissingServletRequestParameterException("page", "int"))
        assertThat(missing.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(body(missing)["code"].asString()).isEqualTo("0x000400-3")

        val mismatch = web.handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException("x", Int::class.java, "page", mock(MethodParameter::class.java), NumberFormatException("x"))
        )
        assertThat(body(mismatch)["code"].asString()).isEqualTo("0x000400-4")

        val method = web.handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException("PATCH"))
        assertThat(method.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(body(method)["message"].asString()).isEqualTo("Invalid method")
    }

    @Test
    fun `validation errors list the offending fields`() {
        val binding = BeanPropertyBindingResult(Any(), "form")
        binding.rejectValue(null, "invalid", "whole form is wrong")
        val fieldBinding = BeanPropertyBindingResult(object { @Suppress("unused") var name: String? = null }, "form")
        fieldBinding.rejectValue("name", "NotBlank", "must not be blank")
        val web = LubbleRestWebExceptionHandler()
        val parameter = mock(MethodParameter::class.java)

        val field = body(web.handleMethodArgumentNotValidException(MethodArgumentNotValidException(parameter, fieldBinding)))
        assertThat(field["code"].asString()).isEqualTo("0x000400-1")
        assertThat(field["message"].asString()).isEqualTo("Invalid parameter")
        assertThat(field["details"]["name"].asString()).isEqualTo("must not be blank")

        // A global error has a single code; it must be reported, not crash the handler.
        val global = web.handleMethodArgumentNotValidException(MethodArgumentNotValidException(parameter, binding))
        assertThat(global.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `payload data and security errors`() {
        val unreadable = LubbleRestConverterExceptionHandler().handleHttpMessageNotReadableException(
            HttpMessageNotReadableException("bad json", MockHttpInputMessage(ByteArray(0)))
        )
        assertThat(unreadable.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(body(unreadable)["code"].asString()).isEqualTo("0x000400-2")

        val violation = mock(ConstraintViolation::class.java)
        val path = mock(Path::class.java)
        `when`(path.toString()).thenReturn("create.name")
        `when`(violation.propertyPath).thenReturn(path)
        val constraint = LubbleRestDataExceptionHandler().handleConstraintViolationException(
            ConstraintViolationException("create.name: global.exception.invalid.param", setOf(violation))
        )
        assertThat(constraint.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(body(constraint)["message"].asString()).isEqualTo("Invalid parameter")
        assertThat(body(constraint)["details"]["fields"][0].asString()).isEqualTo("create.name")

        val denied = LubbleRestSecurityExceptionHandler().handleAccessDenied(AccessDeniedException("nope"))
        assertThat(denied.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(body(denied)["code"].asString()).isEqualTo("0x000403-2")
    }
}
