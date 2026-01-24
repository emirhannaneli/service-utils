package net.lubble.util.helper

import net.lubble.util.AppContextUtil
import net.lubble.util.SpringDetectionUtil
import net.lubble.util.constant.EnumConstant
import net.lubble.util.exception.InvalidParamException
import org.springframework.context.MessageSource
import java.util.*

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
     * If Spring is not available, returns the label as-is.
     * @return The localized label or the original label if Spring is not available.
     */
    fun labelLocalized(): String {
        if (SpringDetectionUtil.isSpringAvailable() && 
            SpringDetectionUtil.isSpringMessageSourceAvailable()) {
            return try {
                val source = AppContextUtil.bean(MessageSource::class.java)
                val locale = getLocale()
                source.getMessage(label, null, locale)
            } catch (e: Exception) {
                // If Spring is available but MessageSource is not configured, return label as-is
                label
            }
        }
        return label
    }

    /**
     * Gets the current locale. Returns default locale if Spring is not available.
     * @return The current locale or default locale.
     */
    private fun getLocale(): Locale {
        return if (SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
            try {
                val localeHolderClass = Class.forName("org.springframework.context.i18n.LocaleContextHolder")
                val getLocaleMethod = localeHolderClass.getMethod("getLocale")
                @Suppress("UNCHECKED_CAST")
                getLocaleMethod.invoke(null) as Locale
            } catch (e: Exception) {
                Locale.getDefault()
            }
        } else {
            Locale.getDefault()
        }
    }
}