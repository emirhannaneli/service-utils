package net.lubble.util.annotation.upper

import com.fasterxml.jackson.databind.util.StdConverter
import org.springframework.context.i18n.LocaleContextHolder
import java.util.*

class UpperProcessor : StdConverter<String, String?>() {

    companion object {
        fun handle(value: String) = value.uppercase()
        fun handle(value: String, locale: Locale) = value.uppercase(locale)
    }

    override fun convert(p0: String?): String? {
        val locale = LocaleContextHolder.getLocale()
        return p0?.let { handle(it, locale) }
    }
}