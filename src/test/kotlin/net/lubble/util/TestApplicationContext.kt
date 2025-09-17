package net.lubble.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.lubble.util.config.lubble.LubbleConfig
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.context.support.registerBean
import java.util.Locale

object TestApplicationContext {
    private val messageSource = StaticMessageSource().apply {
        setUseCodeAsDefaultMessage(true)
    }

    private val lubbleConfig = LubbleConfig().apply {
        security = LubbleConfig.Security().apply {
            cookie = LubbleConfig.Security.Cookie().apply {
                prefix = "lubble-"
            }
        }
        oauth2 = LubbleConfig.OAuth2().apply {
            google = LubbleConfig.OAuth2Credentials().apply {
                clientId = "client-id"
                clientSecret = "client-secret"
                issuers = listOf("https://issuer.example")
            }
        }
    }

    init {
        LocaleContextHolder.setLocale(Locale.ENGLISH)

        val context = GenericApplicationContext().apply {
            registerBean(MessageSource::class.java) { messageSource }
            registerBean(ObjectMapper::class.java) { jacksonObjectMapper() }
            registerBean(LubbleConfig::class.java) { lubbleConfig }
            registerBean("greetingBean", String::class.java) { "hello-world" }
            refresh()
        }

        AppContextUtil.initialize(context)
    }

    fun addMessage(code: String, message: String, locale: Locale = Locale.ENGLISH) {
        messageSource.addMessage(code, locale, message)
    }

    fun updateCookiePrefix(prefix: String) {
        lubbleConfig.security.cookie.prefix = prefix
    }
}
