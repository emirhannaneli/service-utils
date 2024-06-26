package net.lubble.util.handler

import jakarta.annotation.Priority
import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@Priority(0)
@ControllerAdvice
@ConditionalOnClass(
    value = [
        HttpMessageNotReadableException::class
    ]
)
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
class LubbleRestConverterExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<Response> {
        return Response(
            "global.exception.invalid.payload",
            BAD_REQUEST,
            "0x000400-2",
            mapOf(
                "message" to e.message
            )
        ).build()
    }
}