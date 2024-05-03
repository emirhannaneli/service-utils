package net.lubble.util.handler

import net.lubble.util.Response
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.exception.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.http.HttpStatus.*
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
    fun handleException(e: RuntimeException): Response {
        return Response(
            "global.exception.internal.error",
            INTERNAL_SERVER_ERROR,
            "0x000500-0",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(java.lang.UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(e: UnsupportedOperationException): Response {
        return Response(
            "global.exception.unsupported.operation",
            INTERNAL_SERVER_ERROR,
            "0x000500-1",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(InvalidParamException::class)
    fun handleInvalidParamException(e: InvalidParamException): Response {
        return Response(e)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): Response {
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
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): Response {
        return Response(
            "global.exception.invalid.payload",
            BAD_REQUEST,
            "0x000400-2",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): Response {
        return Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-3",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): Response {
        return Response(
            "global.exception.invalid.param",
            BAD_REQUEST,
            "0x000400-4",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): Response {
        return Response(
            "global.exception.invalid.method",
            BAD_REQUEST,
            "0x000400-5",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(PropertyReferenceException::class)
    fun handlePropertyReferenceException(e: PropertyReferenceException): Response {
        return Response(
            "global.exception.invalid.param.type",
            BAD_REQUEST,
            "0x000400-6",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(e: NotFoundException): Response {
        return Response(e, e.details())
    }

    @ExceptionHandler(AlreadyExistsException::class)
    fun handleAlreadyExistsException(e: AlreadyExistsException): Response {
        return Response(e, e.details())
    }

    @ExceptionHandler(WrongCredentials::class)
    fun handleWrongCredentials(e: WrongCredentials): Response {
        return Response(e)
    }

    @ExceptionHandler(AccessDenied::class)
    fun handleAccessDenied(e: AccessDenied): Response {
        return Response(e, e.details() ?: "unknown")
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleAccessDenied(e: org.springframework.security.access.AccessDeniedException): Response {
        return Response(
            "global.exception.access.denied",
            FORBIDDEN,
            "0x000403-2",
            mapOf(
                "message" to e.message
            )
        )
    }

    @ExceptionHandler(UnAuthorized::class)
    fun handleUnAuthorized(e: UnAuthorized): Response {
        return Response(e)
    }

    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplementedError(e: NotImplementedError): Response {
        return Response(
            "global.exception.unsupported.operation",
            NOT_IMPLEMENTED,
            "0x000500-2",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
    }
}