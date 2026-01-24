package net.lubble.util

import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory

/**
 * AppContextUtil is a utility class that provides methods to retrieve beans from the application context.
 * It is initialized with an instance of ApplicationContext when Spring is available.
 * 
 * When Spring is not available, this utility will log warnings and throw appropriate exceptions.
 *
 * @property context The ApplicationContext instance this utility class operates on (only available when Spring is present).
 * @property log The logger instance used for logging.
 */
class AppContextUtil private constructor(private val context: Any?) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    /**
     * Logs a message indicating that the AppContextUtil has been initialized.
     */
    init {
        if (SpringDetectionUtil.isSpringAvailable()) {
            log.info("Lubble Utils AppContextUtil initialized with Spring ApplicationContext.")
        } else {
            log.warn("Lubble Utils AppContextUtil initialized without Spring. Spring features will not be available.")
        }
    }

    companion object {
        @Volatile
        var instance: AppContextUtil? = null

        /**
         * Initializes the AppContextUtil with an instance of ApplicationContext.
         * This method should only be called when Spring is available.
         *
         * @param context The ApplicationContext instance to initialize the AppContextUtil with.
         */
        fun initialize(context: Any) {
            if (!SpringDetectionUtil.isSpringAvailable()) {
                val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
                log.warn("Attempted to initialize AppContextUtil with Spring ApplicationContext, but Spring is not available in classpath.")
                return
            }
            instance = AppContextUtil(context)
        }

        private val safeContext: Any
            get() {
                if (!SpringDetectionUtil.isSpringAvailable()) {
                    throw IllegalStateException(
                        "Spring Framework is not available in classpath. " +
                        "AppContextUtil requires Spring to be initialized. " +
                        "Please ensure Spring is in your classpath or use @EnableLubbleUtils annotation in a Spring application."
                    )
                }
                return instance?.context ?: throw IllegalStateException(
                    "AppContextUtil instance or context is not initialized. " +
                    "Please ensure @EnableLubbleUtils annotation is used in your Spring application configuration."
                )
            }

        /**
         * Retrieves a bean from the application context by its class.
         *
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws IllegalStateException if Spring is not available or AppContextUtil is not initialized.
         */
        fun <T : Any> bean(clazz: Class<T>): T {
            val context = safeContext
            return try {
                val getBeanMethod = context.javaClass.getMethod("getBean", Class::class.java)
                @Suppress("UNCHECKED_CAST")
                getBeanMethod.invoke(context, clazz) as T
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean of type ${clazz.name}", e)
            }
        }

        /**
         * Retrieves a bean from the application context by its name and class.
         *
         * @param name The name of the bean to retrieve.
         * @param clazz The class of the bean to retrieve.
         * @return The bean instance.
         * @throws IllegalStateException if Spring is not available or AppContextUtil is not initialized.
         */
        fun <T : Any> bean(name: String, clazz: Class<T>): T {
            val context = safeContext
            return try {
                val getBeanMethod = context.javaClass.getMethod("getBean", String::class.java, Class::class.java)
                @Suppress("UNCHECKED_CAST")
                getBeanMethod.invoke(context, name, clazz) as T
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean '$name' of type ${clazz.name}", e)
            }
        }

        /**
         * Retrieves a bean from the application context by its name.
         *
         * @param name The name of the bean to retrieve.
         * @return The bean instance.
         * @throws IllegalStateException if Spring is not available or AppContextUtil is not initialized.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> bean(name: String): T {
            val context = safeContext
            return try {
                val getBeanMethod = context.javaClass.getMethod("getBean", String::class.java)
                getBeanMethod.invoke(context, name) as T
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve bean '$name'", e)
            }
        }

        /**
         * Retrieves all beans of a given class from the application context.
         *
         * @param clazz The class of the beans to retrieve.
         * @return A map of bean names to bean instances.
         * @throws IllegalStateException if Spring is not available or AppContextUtil is not initialized.
         */
        fun <T : Any> beans(clazz: Class<T>): Map<String, T> {
            val context = safeContext
            return try {
                val getBeansOfTypeMethod = context.javaClass.getMethod("getBeansOfType", Class::class.java)
                @Suppress("UNCHECKED_CAST")
                getBeansOfTypeMethod.invoke(context, clazz) as Map<String, T>
            } catch (e: Exception) {
                throw IllegalStateException("Failed to retrieve beans of type ${clazz.name}", e)
            }
        }
    }
}