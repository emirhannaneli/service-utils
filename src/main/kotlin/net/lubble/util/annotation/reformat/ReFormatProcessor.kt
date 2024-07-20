package net.lubble.util.annotation.reformat

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.FieldSignature
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.*

@Aspect
@Component
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
            formatted = if (useContextLocale) {
                try {
                    val contextLocale = LocaleContextHolder.getLocale()
                    value.uppercase(contextLocale)
                } catch (e: Exception) {
                    value.uppercase(Locale.forLanguageTag(locale))
                }
            } else {
                value.uppercase(Locale.forLanguageTag(locale))
            }
        }

        if (lower) {
            formatted = if (useContextLocale) {
                try {
                    val contextLocale = LocaleContextHolder.getLocale()
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
            formatted = if (useContextLocale) {
                try {
                    val contextLocale = LocaleContextHolder.getLocale()
                    value.replaceFirstChar { it.titlecase(contextLocale) }
                } catch (e: Exception) {
                    value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                }
            } else {
                value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
            }
        }

        if (decapitate) {
            formatted = if (useContextLocale) {
                try {
                    val contextLocale = LocaleContextHolder.getLocale()
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
}