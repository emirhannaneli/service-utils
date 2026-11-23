package net.lubble.util.annotation.reformat

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.FieldSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.lang.reflect.Field
import java.util.*

@Aspect
@Component
@ConditionalOnClass(name = ["org.springframework.context.i18n.LocaleContextHolder"])
class ReFormatProcessor {

    @Around("@annotation(net.lubble.util.annotation.reformat.ReFormat)")
    fun reformat(point: ProceedingJoinPoint): Any? {
        val value = point.proceed()
        if (value !is String) return value
        val field = field(point) ?: return value
        val reFormat = field.getAnnotation(ReFormat::class.java) ?: return value
        return apply(value, reFormat)
    }

    /**
     * Applies the requested transformations in a fixed order (upper, lower, trim, capitalize,
     * decapitate), each one working on the result of the previous one.
     */
    internal fun apply(value: String, reFormat: ReFormat): String {
        val locale = if (reFormat.useContextLocale) LocaleContextHolder.getLocale()
        else Locale.forLanguageTag(reFormat.locale)

        var formatted = value
        if (reFormat.upper) formatted = formatted.uppercase(locale)
        if (reFormat.lower) formatted = formatted.lowercase(locale)
        if (reFormat.trim) formatted = formatted.trim()
        if (reFormat.capitalize) formatted = formatted.replaceFirstChar { it.titlecase(locale) }
        if (reFormat.decapitate) formatted = formatted.replaceFirstChar { it.lowercase(locale) }
        return formatted
    }

    private fun field(point: ProceedingJoinPoint): Field? {
        val signature = point.signature
        return if (signature is FieldSignature)
            signature.declaringType.getDeclaredField(signature.name)
        else null
    }
}
