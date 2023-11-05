package net.lubble.util.model

import net.lubble.util.AppContextUtil
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import java.util.*

/**
 * Interface for exceptions
 * */
interface ExceptionModel {
    fun message(): String
    fun status(): HttpStatus
    fun code(): String

    fun source(): MessageSource {
        return AppContextUtil.bean(MessageSource::class.java)
    }

    fun locale(): Locale {
        return LocaleContextHolder.getLocale()
    }
}