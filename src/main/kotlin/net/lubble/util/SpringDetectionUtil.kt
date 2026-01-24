package net.lubble.util

import org.slf4j.LoggerFactory

/**
 * Utility class for detecting Spring framework availability at runtime.
 * Uses reflection to safely check if Spring classes are available in the classpath.
 */
object SpringDetectionUtil {
    private val log = LoggerFactory.getLogger(SpringDetectionUtil::class.java)
    
    private var springAvailable: Boolean? = null
    private var springSecurityAvailable: Boolean? = null
    private var springWebAvailable: Boolean? = null
    private var springDataAvailable: Boolean? = null
    
    /**
     * Checks if Spring Framework (ApplicationContext) is available in the classpath.
     * @return true if Spring is available, false otherwise
     */
    fun isSpringAvailable(): Boolean {
        if (springAvailable == null) {
            springAvailable = try {
                Class.forName("org.springframework.context.ApplicationContext")
                true
            } catch (e: ClassNotFoundException) {
                log.debug("Spring Framework not found in classpath")
                false
            } catch (e: Exception) {
                log.debug("Error checking for Spring Framework: ${e.message}")
                false
            }
        }
        return springAvailable ?: false
    }
    
    /**
     * Checks if Spring Security is available in the classpath.
     * @return true if Spring Security is available, false otherwise
     */
    fun isSpringSecurityAvailable(): Boolean {
        if (springSecurityAvailable == null) {
            springSecurityAvailable = try {
                Class.forName("org.springframework.security.core.context.SecurityContextHolder")
                true
            } catch (e: ClassNotFoundException) {
                log.debug("Spring Security not found in classpath")
                false
            } catch (e: Exception) {
                log.debug("Error checking for Spring Security: ${e.message}")
                false
            }
        }
        return springSecurityAvailable ?: false
    }
    
    /**
     * Checks if Spring Web is available in the classpath.
     * @return true if Spring Web is available, false otherwise
     */
    fun isSpringWebAvailable(): Boolean {
        if (springWebAvailable == null) {
            springWebAvailable = try {
                Class.forName("org.springframework.web.bind.annotation.RestController")
                true
            } catch (e: ClassNotFoundException) {
                log.debug("Spring Web not found in classpath")
                false
            } catch (e: Exception) {
                log.debug("Error checking for Spring Web: ${e.message}")
                false
            }
        }
        return springWebAvailable ?: false
    }
    
    /**
     * Checks if Spring Data is available in the classpath.
     * @return true if Spring Data is available, false otherwise
     */
    fun isSpringDataAvailable(): Boolean {
        if (springDataAvailable == null) {
            springDataAvailable = try {
                Class.forName("org.springframework.data.domain.Pageable")
                true
            } catch (e: ClassNotFoundException) {
                log.debug("Spring Data not found in classpath")
                false
            } catch (e: Exception) {
                log.debug("Error checking for Spring Data: ${e.message}")
                false
            }
        }
        return springDataAvailable ?: false
    }
    
    /**
     * Checks if Spring MessageSource is available in the classpath.
     * @return true if Spring MessageSource is available, false otherwise
     */
    fun isSpringMessageSourceAvailable(): Boolean {
        return try {
            Class.forName("org.springframework.context.MessageSource")
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if Spring LocaleContextHolder is available in the classpath.
     * @return true if Spring LocaleContextHolder is available, false otherwise
     */
    fun isSpringLocaleContextHolderAvailable(): Boolean {
        return try {
            Class.forName("org.springframework.context.i18n.LocaleContextHolder")
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Resets all cached detection results. Useful for testing.
     */
    fun reset() {
        springAvailable = null
        springSecurityAvailable = null
        springWebAvailable = null
        springDataAvailable = null
    }
}
