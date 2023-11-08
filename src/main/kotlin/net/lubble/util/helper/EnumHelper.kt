package net.lubble.util.helper

import net.lubble.util.AppContextUtil
import net.lubble.util.constant.EnumConstant
import net.lubble.util.exception.InvalidParamException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Interface to be implemented by enums that will be used in the frontend.
 * */
interface EnumHelper {
    val label: String
    val value: String
    val color: String?
    val icon: String?

    companion object {
        inline fun <reified T : Enum<T>> findByName(name: String): T {
            return enumValues<T>().find { it.name == name }
                ?: throw InvalidParamException(
                    "Invalid enum value (" + T::class.java.getSimpleName() + ")",
                    name,
                    T::class.java.enumConstants
                )
        }

        inline fun <reified T> fromValue(value: String): T where T : Enum<T>, T : EnumHelper {
            return enumValues<T>().find { it.value == value }
                ?: throw InvalidParamException(
                    "Invalid enum value (${T::class.java.simpleName})",
                    value,
                    enumValues<T>().map { it.value }
                )
        }

    }

    fun toConstant(): EnumConstant {
        return EnumConstant(labelLocalized(), value, color, icon)
    }

    fun labelLocalized(): String {
        val source = AppContextUtil.bean(MessageSource::class.java)
        val locale = LocaleContextHolder.getLocale()
        return source.getMessage(label, null, locale)
    }
}