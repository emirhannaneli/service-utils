package net.lubble.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for detecting Spring framework availability at runtime.
 * Each classpath probe runs once; the result is cached until [reset] is called.
 */
object SpringDetectionUtil {
    private val cache = ConcurrentHashMap<String, Boolean>()

    private fun isPresent(className: String): Boolean = cache.computeIfAbsent(className) {
        try {
            Class.forName(it, false, SpringDetectionUtil::class.java.classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    /** Checks if Spring Framework (spring-context) is available in the classpath. */
    fun isSpringAvailable(): Boolean = isPresent("org.springframework.context.ApplicationContext")

    /** Checks if Spring Security is available in the classpath. */
    fun isSpringSecurityAvailable(): Boolean =
        isPresent("org.springframework.security.core.context.SecurityContextHolder")

    /** Checks if Spring Web is available in the classpath. */
    fun isSpringWebAvailable(): Boolean = isPresent("org.springframework.web.bind.annotation.RestController")

    /** Checks if Spring Data is available in the classpath. */
    fun isSpringDataAvailable(): Boolean = isPresent("org.springframework.data.domain.Pageable")

    /** Checks if Spring MessageSource is available in the classpath. */
    fun isSpringMessageSourceAvailable(): Boolean = isPresent("org.springframework.context.MessageSource")

    /** Checks if Spring LocaleContextHolder is available in the classpath. */
    fun isSpringLocaleContextHolderAvailable(): Boolean =
        isPresent("org.springframework.context.i18n.LocaleContextHolder")

    /** Clears the cached detection results (useful for testing). */
    fun reset() {
        cache.clear()
    }
}
