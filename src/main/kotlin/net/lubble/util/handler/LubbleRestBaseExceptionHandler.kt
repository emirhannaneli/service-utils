package net.lubble.util.handler

import jakarta.annotation.Priority
import net.lubble.util.Response
import net.lubble.util.config.lubble.LubbleConfig
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.exception.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@Priority(1)
@ControllerAdvice
@ConditionalOnProperty(prefix = "lubble", name = ["exception-handling"], havingValue = "true", matchIfMissing = true)
class LubbleRestBaseExceptionHandler {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    /** Optional so the handler can still be instantiated by hand (e.g. standalone MockMvc tests). */
    @Autowired(required = false)
    var config: LubbleConfig? = null

    init {
        log.info("Lubble Utils Exception Handlers initialized.")
    }

    /**
     * Exception class and message of an unexpected error. Only sent to the client when
     * `lubble.expose-exception-details` is enabled; the error itself is always logged.
     */
    private fun details(e: Throwable): Map<String, String?>? {
        if (config?.exposeExceptionDetails != true) return null
        return mapOf(
            "exception" to e.javaClass.name,
            "message" to e.message
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleException(e: RuntimeException): ResponseEntity<Response> {
        log.error("Unhandled exception", e)
        return Response(
            "global.exception.internal.error",
            INTERNAL_SERVER_ERROR,
            "0x000500-0",
            details(e)
        ).build()
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(e: UnsupportedOperationException): ResponseEntity<Response> {
        return Response(
            "global.exception.unsupported.operation",
            INTERNAL_SERVER_ERROR,
            "0x000500-1",
            details(e)
        ).build()
    }

    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplementedError(e: NotImplementedError): ResponseEntity<Response> {
        return Response(
            "global.exception.unsupported.operation",
            NOT_IMPLEMENTED,
            "0x000500-2",
            details(e)
        ).build()
    }

    @ExceptionHandler(InvalidParamException::class)
    fun handleInvalidParamException(e: InvalidParamException): ResponseEntity<Response> {
        return Response(e).build()
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): ResponseEntity<Response> {
        return Response(e, e.details()).build()
    }

    @ExceptionHandler(AlreadyExistsException::class)
    fun handleAlreadyExistsException(e: AlreadyExistsException): ResponseEntity<Response> {
        return Response(e, e.details()).build()
    }

    @ExceptionHandler(WrongCredentials::class)
    fun handleWrongCredentials(e: WrongCredentials): ResponseEntity<Response> {
        return Response(e).build()
    }

    @ExceptionHandler(AccessDenied::class)
    fun handleAccessDenied(e: AccessDenied): ResponseEntity<Response> {
        return Response(e, e.details() ?: "unknown").build()
    }

    @ExceptionHandler(UnAuthorized::class)
    fun handleUnAuthorized(e: UnAuthorized): ResponseEntity<Response> {
        return Response(e).build()
    }
}