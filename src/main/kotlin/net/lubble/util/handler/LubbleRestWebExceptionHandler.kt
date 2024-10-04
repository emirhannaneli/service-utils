package net.lubble.util.handler

import jakarta.annotation.Priority
import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@Priority(0)
@ControllerAdvice
@ConditionalOnClass(
    value = [
        MethodArgumentNotValidException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class,
        HttpRequestMethodNotSupportedException::class
    ]
)
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
class LubbleRestWebExceptionHandler {
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
        return Response(
            message.toString(),
            BAD_REQUEST,
            "0x000400-1",
            details
        ).build()
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<Response> {
        return Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-3",
            mapOf(
                "message" to e.message
            )
        ).build()
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<Response> {
        return Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-4",
            mapOf(
                "message" to e.message
            )
        ).build()
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<Response> {
        return Response(
            "global.exception.invalid.method",
            BAD_REQUEST,
            "0x000400-5",
            mapOf(
                "message" to e.message
            )
        ).build()
    }
}