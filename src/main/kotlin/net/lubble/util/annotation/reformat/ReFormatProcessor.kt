package net.lubble.util.annotation.reformat

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.util.*

@Aspect
@Component
class ReFormatProcessor {
    @Around("@annotation(net.lubble.util.annotation.reformat.ReFormat)")
    fun process(point: ProceedingJoinPoint): Any {
        val args = point.args
        for (i in args.indices) {
            val arg = args[i]
            if (arg is String) {
                val annotation = point.signature.declaringType.getDeclaredField(point.signature.name).getAnnotation(ReFormat::class.java)
                val upper = annotation.upper
                val lower = annotation.lower
                val trim = annotation.trim
                val capitalize = annotation.capitalize
                val decapitate = annotation.decapitate
                val locale = annotation.locale
                val useContextLocale = annotation.useContextLocale
                var value = arg
                if (upper) {
                    if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value = value.uppercase(contextLocale)
                        } catch (e: Exception) {
                            value = value.uppercase(Locale.forLanguageTag(locale))
                        }
                    } else value = value.uppercase(Locale.forLanguageTag(locale))

                }
                if (lower) {
                    if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value = value.lowercase(contextLocale)
                        } catch (e: Exception) {
                            value = value.lowercase(Locale.forLanguageTag(locale))
                        }
                    } else {
                        value = value.lowercase(Locale.forLanguageTag(locale))
                    }
                }
                if (trim) {
                    value = value.trim()
                }
                if (capitalize) {
                    if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value = value.replaceFirstChar { it.titlecase(contextLocale) }
                        } catch (e: Exception) {
                            value = value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                        }
                    } else {
                        value = value.replaceFirstChar { it.titlecase(Locale.forLanguageTag(locale)) }
                    }
                }
                if (decapitate) {
                    if (useContextLocale) {
                        try {
                            val contextLocale = LocaleContextHolder.getLocale()
                            value = value.replaceFirstChar { it.lowercase(contextLocale) }
                        } catch (e: Exception) {
                            value = value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
                        }
                    } else {
                        value = value.replaceFirstChar { it.lowercase(Locale.forLanguageTag(locale)) }
                    }
                }
                args[i] = value
            }
        }
        return point.proceed(args)
    }
}