package net.lubble.util

import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import java.util.Locale

/**
 * AppContextUtil is a utility class that provides methods to retrieve beans from the application context.
 * It is initialized with an instance of ApplicationContext when Spring is available.
 *
 * When Spring is not available, this utility will log warnings and throw appropriate exceptions.
 *
 * @property context The ApplicationContext instance this utility class operates on (only available when Spring is present).
 */
class AppContextUtil private constructor(private val context: Any?) {

    companion object {
        private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

        @Volatile
        var instance: AppContextUtil? = null

        /**
         * Initializes the AppContextUtil with a Spring ApplicationContext.
         * This method should only be called when Spring is available.
         *
         * @param context The Spring ApplicationContext instance.
         */
        fun initialize(context: Any) {
            if (!SpringDetectionUtil.isSpringAvailable()) {
                log.warn("Attempted to initialize AppContextUtil with Spring ApplicationContext, but Spring is not available in classpath.")
                return
            }
            instance = AppContextUtil(context)
            log.info("Lubble Utils AppContextUtil initialized with Spring ApplicationContext.")
        }

        private val safeContext: BeanFactory
            get() {
                if (!SpringDetectionUtil.isSpringAvailable()) {
                    throw IllegalStateException(
                        "Spring Framework is not available in classpath. " +
                                "AppContextUtil requires Spring to be initialized. " +
                                "Please ensure Spring is in your classpath or use @EnableLubbleUtils annotation in a Spring application."
                    )
                }
                return instance?.context as? BeanFactory ?: throw IllegalStateException(
                    "AppContextUtil instance or context is not initialized. " +
                            "Please ensure @EnableLubbleUtils annotation is used in your Spring application configuration."
                )
            }

        /**
         * Retrieves a bean of the specified class from the application context.
         *
         * @param clazz The class of the bean to retrieve.
         * @return The bean of the specified class.
         * @throws IllegalStateException if Spring is not available, the context is not initialized or the lookup fails.
         */
        fun <T : Any> bean(clazz: Class<T>): T {
            val context = safeContext
            return try {
                context.getBean(clazz)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean of type ${clazz.name}", e)
            }
        }

        /**
         * Retrieves a bean of the specified name and class from the application context.
         *
         * @param name The name of the bean to retrieve.
         * @param clazz The class of the bean to retrieve.
         * @return The bean of the specified name and class.
         * @throws IllegalStateException if Spring is not available, the context is not initialized or the lookup fails.
         */
        fun <T : Any> bean(name: String, clazz: Class<T>): T {
            val context = safeContext
            return try {
                context.getBean(name, clazz)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean '$name' of type ${clazz.name}", e)
            }
        }

        /**
         * Retrieves a bean of the specified name from the application context.
         *
         * @param name The name of the bean to retrieve.
         * @return The bean of the specified name.
         * @throws IllegalStateException if Spring is not available, the context is not initialized or the lookup fails.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> bean(name: String): T {
            val context = safeContext
            return try {
                context.getBean(name) as T
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean '$name'", e)
            }
        }

        /**
         * Retrieves all beans of the specified class from the application context.
         *
         * @param clazz The class of the beans to retrieve.
         * @return A map of bean names to beans of the specified class.
         * @throws IllegalStateException if Spring is not available, the context is not initialized or the lookup fails.
         */
        fun <T : Any> beans(clazz: Class<T>): Map<String, T> {
            val context = safeContext
            return try {
                (context as ListableBeanFactory).getBeansOfType(clazz)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve beans of type ${clazz.name}", e)
            }
        }

        /** The locale of the current request/thread, or the JVM default when Spring is not available. */
        internal fun locale(): Locale =
            if (SpringDetectionUtil.isSpringAvailable()) LocaleContextHolder.getLocale() else Locale.getDefault()

        /**
         * Resolves [code] through the application's [MessageSource].
         * Returns [code] unchanged when there is no context, no MessageSource or no such message.
         */
        internal fun localize(code: String): String {
            if (!SpringDetectionUtil.isSpringAvailable() || instance == null) return code
            return try {
                bean(MessageSource::class.java).getMessage(code, null, code, locale()) ?: code
            } catch (_: Exception) {
                code
            }
        }
    }
}
