package net.lubble.util.model

import net.lubble.util.AppContextUtil
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import java.util.*

/**
 * This interface represents a model for exceptions.
 * It provides methods to get the message, status, and code of the exception.
 * It also provides methods to get the MessageSource bean and the current locale.
 */
interface ExceptionModel {
    /**
     * Returns the message of the exception.
     * @return The message.
     */
    fun message(): String

    /**
     * Returns the HTTP status of the exception.
     * @return The HTTP status.
     */
    fun status(): HttpStatus

    /**
     * Returns the code of the exception.
     * @return The code.
     */
    fun code(): String

    /**
     * Retrieves the MessageSource bean from the application context.
     * @return The MessageSource bean.
     */
    fun source(): MessageSource {
        return AppContextUtil.bean(MessageSource::class.java)
    }

    /**
     * Retrieves the current locale.
     * @return The current locale.
     */
    fun locale(): Locale {
        return LocaleContextHolder.getLocale()
    }
}