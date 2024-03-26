package net.lubble.util

import org.springframework.context.i18n.LocaleContextHolder
import java.util.*

class ReFormat(private var value: String?) {
    private val locale = LocaleContextHolder.getLocale() ?: Locale.ENGLISH
    fun upper(): ReFormat {
        value = value?.uppercase(locale)
        return this
    }

    fun upper(locale: Locale): ReFormat {
        value = value?.uppercase(locale)
        return this
    }


    fun lower(): ReFormat {
        value = value?.lowercase(locale)
        return this
    }

    fun lower(locale: Locale): ReFormat {
        value = value?.lowercase(locale)
        return this
    }


    fun trim(): ReFormat {
        value = value?.trim()
        return this
    }

    fun capitalize(): ReFormat {
        value = value?.replaceFirstChar { it.titlecase(locale) }
        return this
    }

    fun capitalize(locale: Locale): ReFormat {
        value = value?.replaceFirstChar { it.titlecase(locale) }
        return this
    }

    fun decapitate(): ReFormat {
        value = value?.replaceFirstChar { it.lowercase(locale) }
        return this
    }

    fun decapitate(locale: Locale): ReFormat {
        value = value?.replaceFirstChar { it.lowercase(locale) }
        return this
    }

    fun format(): String? {
        return value
    }
}