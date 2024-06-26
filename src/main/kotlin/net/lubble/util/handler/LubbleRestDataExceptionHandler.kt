package net.lubble.util.handler

import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
@ConditionalOnClass(
    name = [
        "net.lubble.util.handler.LubbleRestBaseExceptionHandler",
        "org.springframework.data.mapping.PropertyReferenceException"
    ]
)
class LubbleRestDataExceptionHandler {
    @ExceptionHandler(PropertyReferenceException::class)
    fun handlePropertyReferenceException(e: PropertyReferenceException): ResponseEntity<Response> {
        return Response(
            "global.exception.invalid.param.type",
            BAD_REQUEST,
            "0x000400-6",
            mapOf(
                "message" to e.message
            )
        ).build()
    }
}