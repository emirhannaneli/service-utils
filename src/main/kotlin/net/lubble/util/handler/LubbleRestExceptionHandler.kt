package net.lubble.util.handler

import net.lubble.util.Response
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.exception.AlreadyExistsException
import net.lubble.util.exception.InvalidParamException
import net.lubble.util.exception.NotFoundException
import net.lubble.util.exception.WrongCredentials
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@ControllerAdvice
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
class LubbleRestExceptionHandler {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    init {
        log.info("Lubble Utils LubbleRestExceptionHandler initialized.")
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleException(e: RuntimeException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.internal.error",
            INTERNAL_SERVER_ERROR,
            "0x000500-0",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(java.lang.UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(e: UnsupportedOperationException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.unsupported.operation",
            INTERNAL_SERVER_ERROR,
            "0x000500-1",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(InvalidParamException::class)
    fun handleInvalidParamException(e: InvalidParamException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.invalid.param",
            e.status(),
            e.code(),
            e.details()
        )
        return response.build()
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<Response> {
        val errors = e.bindingResult.allErrors
        val details = mutableMapOf<String, Any>()
        errors.forEach {
            val field = it.codes?.get(1)?.split(".")?.last() ?: "unknown"
            val message = it.defaultMessage ?: "unknown"
            details[field] = message
        }
        val message = details["unknown"] ?: "global.exception.invalid.param"
        val response = Response(
            message.toString(),
            BAD_REQUEST,
            "0x000400-1",
            details
        )
        return response.build()
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.invalid.payload",
            BAD_REQUEST,
            "0x000400-2",
            mapOf(
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-3",
            mapOf(
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-4",
            mapOf(
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<Response> {
        val response = Response(
            "global.exception.invalid.method",
            BAD_REQUEST,
            "0x000400-5",
            mapOf(
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<Response> {
        val response = Response(
            e.message(),
            e.status(),
            e.code(),
            e.details()
        )
        return response.build()
    }

    @ExceptionHandler(AlreadyExistsException::class)
    fun handleAlreadyExistsException(e: AlreadyExistsException): ResponseEntity<Response> {
        val response = Response(
            e.message(),
            e.status(),
            e.code(),
            e.details()
        )
        return response.build()
    }

    @ExceptionHandler(WrongCredentials::class)
    fun handleWrongCredentials(e: WrongCredentials): ResponseEntity<Response> {
        val response = Response(
            e.message(),
            e.status(),
            e.code()
        )
        return response.build()
    }
}