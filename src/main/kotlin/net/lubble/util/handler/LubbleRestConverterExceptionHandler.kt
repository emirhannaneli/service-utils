package net.lubble.util.handler

import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
@ConditionalOnClass(
    name = [
        "net.lubble.util.handler.LubbleRestBaseExceptionHandler",
        "org.springframework.http.converter.HttpMessageNotReadableException"
    ]
)
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