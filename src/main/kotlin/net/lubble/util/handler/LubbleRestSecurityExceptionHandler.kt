package net.lubble.util.handler

import jakarta.annotation.Priority
import net.lubble.util.Response
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@Priority(0)
@ControllerAdvice
@ConditionalOnClass(
    value = [
        AccessDeniedException::class
    ]
)
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
class LubbleRestSecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<Response> {
        return Response(
            "global.exception.access.denied",
            FORBIDDEN,
            "0x000403-2",
            mapOf(
                "message" to e.message
            )
        ).build()
    }
}