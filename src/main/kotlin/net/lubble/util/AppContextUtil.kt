package net.lubble.util

import org.springframework.context.ApplicationContext

class AppContextUtil(context: ApplicationContext) {
    private var context: ApplicationContext? = context

    companion object {
        private var instance: AppContextUtil? = null

        fun initialize(context: ApplicationContext) {
            instance = AppContextUtil(context)
        }

         fun <T> bean(clazz: Class<T>): T {
            return instance?.context?.getBean(clazz) ?: throw RuntimeException("Could not get bean")
        }

        fun <T> bean(name: String, clazz: Class<T>): T {
            return instance?.context?.getBean(name, clazz) ?: throw RuntimeException("Could not get bean")
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> bean(name: String): T {
            return instance?.context?.getBean(name) as T ?: throw RuntimeException("Could not get bean")
        }
    }
}