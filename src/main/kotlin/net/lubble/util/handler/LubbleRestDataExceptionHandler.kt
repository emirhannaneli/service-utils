package net.lubble.util.handler

import jakarta.annotation.Priority
import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@Priority(0)
@ControllerAdvice
@ConditionalOnClass(
    value = [
        PropertyReferenceException::class
    ]
)
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
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