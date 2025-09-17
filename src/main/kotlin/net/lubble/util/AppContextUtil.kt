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
class AppContextUtil(context: ApplicationContext) {
    private var context: ApplicationContext? = context
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    /**
     * Logs a message indicating that the AppContextUtil has been initialized.
     */
    init {
        log.info("Lubble Utils AppContextUtil initialized.")
    }

    companion object {
        var instance: AppContextUtil? = null

        /**
         * Initializes the AppContextUtil with an instance of ApplicationContext.
         *
         * @param context The ApplicationContext instance to initialize the AppContextUtil with.
         */
        fun initialize(context: ApplicationContext) {
            instance = AppContextUtil(context)
        }

        /**
         * Retrieves a bean from the application context by its class.
         *
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws RuntimeException if the bean could not be retrieved.
         */
        fun <T> bean(clazz: Class<T>): T {
            val ctx = instance?.context ?: throw RuntimeException("Could not get bean (${clazz.name})")
            return try {
                ctx.getBean(clazz)
            } catch (ex: Exception) {
                throw RuntimeException("Could not get bean (${clazz.name})", ex)
            }
        }

        /**
         * Retrieves a bean from the application context by its name and class.
         *
         * @param name The name of the bean to retrieve.
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws RuntimeException if the bean could not be retrieved.
         */
        fun <T> bean(name: String, clazz: Class<T>): T {
            val ctx = instance?.context ?: throw RuntimeException("Could not get bean (${clazz.name})")
            return try {
                ctx.getBean(name, clazz)
            } catch (ex: Exception) {
                throw RuntimeException("Could not get bean (${clazz.name})", ex)
            }
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
            val ctx = instance?.context ?: throw RuntimeException("Could not get bean ($name)")
            return try {
                val bean = ctx.getBean(name)
                (bean as? T) ?: throw RuntimeException("Could not get bean ($name)")
            } catch (ex: Exception) {
                throw RuntimeException("Could not get bean ($name)", ex)
            }
        }

        /**
         * Retrieves all beans of a given class from the application context.
         *
         * @param clazz The class of the beans to retrieve.
         * @return A map of bean names to bean instances.
         */
        fun <T> beans(clazz: Class<T>): Map<String, T> {
            return instance?.context?.getBeansOfType(clazz) ?: emptyMap()
        }
    }
}