package net.lubble.util.annotation.reformat

import net.lubble.util.SpringDetectionUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.FieldSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.*

@Aspect
@Component
@ConditionalOnClass(name = ["org.springframework.context.i18n.LocaleContextHolder"])
class ReFormatProcessor {

    @Around("@annotation(net.lubble.util.annotation.reformat.ReFormat)")
    fun reformat(point: ProceedingJoinPoint): Any {
        println("Reformatting")
        val value = point.proceed()
        if (value !is String) return value
        val field = field(point) ?: return value
        val reFormat = field.getAnnotation(ReFormat::class.java) ?: return value
        return apply(value, reFormat)
    }

    private fun apply(value: String, reFormat: ReFormat): String {
        var formatted = value
        val upper = reFormat.upper
        val lower = reFormat.lower
        val trim = reFormat.trim
        val capitalize = reFormat.capitalize
        val decapitate = reFormat.decapitate
        val locale = reFormat.locale
        val useContextLocale = reFormat.useContextLocale

        if (upper) {
            formatted = if (useContextLocale && SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
                try {
                    val contextLocale = getContextLocale()
                    value.uppercase(contextLocale)
                } catch (e: Exception) {
                    value.uppercase(Locale.forLanguageTag(locale))
                }
            } else {
                value.uppercase(Locale.forLanguageTag(locale))
            }
        }

        if (lower) {
            formatted = if (useContextLocale && SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
                try {
                    val contextLocale = getContextLocale()
                    value.lowercase(contextLocale)
                } catch (e: Exception) {
                    value.lowercase(Locale.forLanguageTag(locale))
                }
            } else {
                value.lowercase(Locale.forLanguageTag(locale))
            }
        }

        if (trim) {
            formatted = value.trim()
        }

        if (capitalize) {
            formatted = if (useContextLocale && SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
                try {
                    val contextLocale = getContextLocale()
                    value.replaceFirstChar { it.titlecase(contextLocale) }
                } catch (e: Exception) {
                    value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                }
            } else {
                value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
            }
        }

        if (decapitate) {
            formatted = if (useContextLocale && SpringDetectionUtil.isSpringLocaleContextHolderAvailable()) {
                try {
                    val contextLocale = getContextLocale()
                    value.replaceFirstChar { it.lowercase(contextLocale) }
                } catch (e: Exception) {
                    value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
                }
            } else {
                value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
            }
        }

        return formatted
    }

    private fun field(point: ProceedingJoinPoint): Field? {
        val signature = point.signature
        return if (signature is FieldSignature)
            signature.declaringType.getDeclaredField(signature.name)
        else null
    }

    /**
     * Gets the locale from Spring LocaleContextHolder if available, otherwise returns default locale.
     * @return The current locale or default locale.
     */
    private fun getContextLocale(): Locale {
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