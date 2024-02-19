package net.lubble.util.annotation.lower

import com.fasterxml.jackson.databind.util.StdConverter
import org.springframework.context.i18n.LocaleContextHolder
import java.util.*

class LowerProcessor : StdConverter<String, String>() {
    companion object {
        fun handle(value: String) = value.lowercase()
        fun handle(value: String, locale: Locale) = value.lowercase(locale)
    }

    override fun convert(p0: String): String {
        val locale = LocaleContextHolder.getLocale()
        return handle(p0, locale)
    }
}