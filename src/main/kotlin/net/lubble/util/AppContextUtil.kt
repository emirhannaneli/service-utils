package net.lubble.util

import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

/**
 * AppContextUtil is a utility class that provides methods to retrieve beans from the application context.
 * It is initialized with an instance of ApplicationContext.
 *
 * @property context The ApplicationContext instance this utility class operates on.
 * @property log The logger instance used for logging.
 */
class AppContextUtil(private val context: ApplicationContext) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    /**
     * Logs a message indicating that the AppContextUtil has been initialized.
     */
    init {
        log.info("Lubble Utils AppContextUtil initialized.")
    }

    companion object {
        @Volatile
        var instance: AppContextUtil? = null

        /**
         * Initializes the AppContextUtil with an instance of ApplicationContext.
         *
         * @param context The ApplicationContext instance to initialize the AppContextUtil with.
         */
        fun initialize(context: ApplicationContext) {
            instance = AppContextUtil(context)
        }

        private val safeContext: ApplicationContext
            get() = instance?.context ?: throw RuntimeException("AppContextUtil instance or context is not initialized")

        /**
         * Retrieves a bean from the application context by its class.
         *
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws RuntimeException if the bean could not be retrieved.
         */
        fun <T : Any> bean(clazz: Class<T>): T {
            return safeContext.getBean(clazz) as T
        }

        /**
         * Retrieves a bean from the application context by its name and class.
         *
         * @param name The name of the bean to retrieve.
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws RuntimeException if the bean could not be retrieved.
         */
        fun <T : Any> bean(name: String, clazz: Class<T>): T {
            return safeContext.getBean(name, clazz) as T
        }

        /**
         * Retrieves a bean from the application context by its name.
         *
         * @param name The name of the bean to retrieve.
         * @return The bean instance.
         * @throws RuntimeException if the bean could not be retrieved.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> bean(name: String): T {
            return safeContext.getBean(name) as T
        }

        /**
         * Retrieves all beans of a given class from the application context.
         *
         * @param clazz The class of the beans to retrieve.
         * @return A map of bean names to bean instances.
         */
        fun <T : Any> beans(clazz: Class<T>): Map<String, T> {
            return safeContext.getBeansOfType(clazz)
        }
    }
}