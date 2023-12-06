package net.lubble.util

import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

class AppContextUtil(context: ApplicationContext) {
    private var context: ApplicationContext? = context
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
    init {
        log.info("Lubble Utils AppContextUtil initialized.")
    }

    companion object {
        private var instance: AppContextUtil? = null

        fun initialize(context: ApplicationContext) {
            instance = AppContextUtil(context)
        }

         fun <T> bean(clazz: Class<T>): T {
            return instance?.context?.getBean(clazz) ?: throw RuntimeException("Could not get bean (${clazz.name})")
        }

        fun <T> bean(name: String, clazz: Class<T>): T {
            return instance?.context?.getBean(name, clazz) ?: throw RuntimeException("Could not get bean (${clazz.name})")
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> bean(name: String): T {
            return instance?.context?.getBean(name) as T ?: throw RuntimeException("Could not get bean ($name)")
        }
    }
}