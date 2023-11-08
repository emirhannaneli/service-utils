package net.lubble.util.handler

import net.lubble.util.Response
import net.lubble.util.exception.AlreadyExistsException
import net.lubble.util.exception.InvalidParamException
import net.lubble.util.exception.NotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.stereotype.Component
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.*

@Component("utilsRestExceptionHandler")
@ConditionalOnClass(ControllerAdvice::class)
class RestExceptionHandler {
    @Autowired
    private lateinit var source: MessageSource

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Response> {
        val response = Response(
            source.getMessage("global.exception.internal.error", null, locale()),
            INTERNAL_SERVER_ERROR,
            "0x000500-0",
            mapOf(
                "exception" to e.javaClass.name,
                "message" to e.message
            )
        )
        return response.build()
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(e: UnsupportedOperationException): ResponseEntity<Response> {
        val response = Response(
            source.getMessage("global.exception.unsupported.operation", null, locale()),
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
            source.getMessage("global.exception.invalid.param", null, locale()),
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
        val defaultMessage = source.getMessage("global.exception.invalid.param", null, locale())
        val message = details["unknown"] ?: defaultMessage
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
            source.getMessage("global.exception.invalid.payload", null, locale()),
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
            source.getMessage("global.exception.invalid.param", null, locale()),
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
            source.getMessage("global.exception.invalid.param", null, locale()),
            BAD_REQUEST,
            "0x000400-4",
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

    private fun locale(): Locale {
        return LocaleContextHolder.getLocale()
    }
}