package net.lubble.util.handler

import jakarta.annotation.Priority
import jakarta.validation.ConstraintViolationException
import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.core.PropertyReferenceException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import kotlin.collections.last
import kotlin.text.trim

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

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<Response> {
        val message = e.message?.split(":")?.last()?.trim() ?: "global.exception.invalid.param"
        return Response(
            message,
            BAD_REQUEST,
            "0x000400-6",
            mapOf(
                "fields" to e.constraintViolations.map { it.propertyPath.toString() }
            )
        ).build()
    }
}