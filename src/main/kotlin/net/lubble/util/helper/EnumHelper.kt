package net.lubble.util.helper

import net.lubble.util.AppContextUtil
import net.lubble.util.constant.EnumConstant
import net.lubble.util.exception.InvalidParamException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder

/**
 * Helper interface for enums with label and value.
 * @property label The label of the enum.
 * @property value The value of the enum.
 */
interface EnumHelper {
    val label: String
    val value: String

    companion object {
        /**
         * Finds an enum by its name.
         * @param name The name of the enum.
         * @return The enum.
         * @throws InvalidParamException If the name does not match any enum.
         */
        inline fun <reified T> findByName(name: String): T where T : Enum<T>, T : EnumHelper {
            return enumValues<T>().find { it.name == name }
                ?: throw InvalidParamException(
                    "Invalid enum value (${T::class.java.simpleName})",
                    name,
                    enumValues<T>().map { it.value }
                )
        }

        /**
         * Finds an enum by its value.
         * @param value The value of the enum.
         * @return The enum.
         * @throws InvalidParamException If the value does not match any enum.
         */
        inline fun <reified T> findByValue(value: String): T where T : Enum<T>, T : EnumHelper {
            return enumValues<T>().find { it.value == value }
                ?: throw InvalidParamException(
                    "Invalid enum value (${T::class.java.simpleName})",
                    value,
                    enumValues<T>().map { it.value }
                )
        }
    }

    /**
     * Converts the enum to a constant.
     * @return The constant.
     */
    fun toConstant(): EnumConstant {
        return EnumConstant(labelLocalized(), value)
    }

    /**
     * Retrieves the localized label of the enum.
     * @return The localized label.
     */
    fun labelLocalized(): String {
        val source = AppContextUtil.bean(MessageSource::class.java)
        val locale = LocaleContextHolder.getLocale()
        return source.getMessage(label, null, locale)
    }
}