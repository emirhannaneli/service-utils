package net.lubble.util

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

/**
 * AuthTool is a utility class that provides methods for handling authentication and authorization.
 * It uses Spring Security's SecurityContextHolder to access the authentication and authorization information.
 *
 * When Spring Security is not available, all methods return null or false.
 */
class AuthTool {
    companion object {
        private val log = LoggerFactory.getLogger(AuthTool::class.java)

        private fun available(): Boolean = SpringDetectionUtil.isSpringSecurityAvailable()

        /**
         * Retrieves the principal (usually the user) from the current authentication context.
         *
         * @return The principal if the user is authenticated and Spring Security is available, null otherwise.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> principal(): T? {
            if (!available()) return null
            return Security.authentication()?.principal as? T
        }

        /**
         * Retrieves the credentials from the current authentication context.
         *
         * @return The credentials if the user is authenticated and Spring Security is available, null otherwise.
         */
        fun credentials(): String? {
            if (!available()) return null
            val authentication = Security.authentication() ?: return null
            if (authentication.principal == null) return null
            return authentication.credentials as? String
        }

        /**
         * Checks if the user is authenticated.
         *
         * @return True if the user is authenticated and Spring Security is available, false otherwise.
         */
        fun isAuthenticated(): Boolean {
            if (!available()) return false
            return Security.authentication()?.principal is UserDetails
        }

        /**
         * Checks if the user is anonymous.
         *
         * @return True if the user is anonymous or Spring Security is not available, false otherwise.
         */
        fun isAnonymous(): Boolean {
            if (!available()) return true
            return Security.authentication()?.principal == null
        }

        /**
         * Authorizes the user with the given authorities.
         * This method only works when Spring Security is available.
         *
         * @param givenAuthorities The authorities to grant to the user.
         */
        fun authorize(vararg givenAuthorities: String) {
            if (!available()) {
                log.debug("Spring Security is not available. Cannot authorize user.")
                return
            }
            try {
                Security.authorize(givenAuthorities)
            } catch (e: Exception) {
                log.warn("Failed to authorize user: ${e.message}", e)
            }
        }
    }

    /**
     * Everything that links against Spring Security lives here, so the classes are only resolved
     * after [SpringDetectionUtil.isSpringSecurityAvailable] has confirmed they exist.
     */
    private object Security {
        fun authentication(): Authentication? = SecurityContextHolder.getContext().authentication

        fun authorize(givenAuthorities: Array<out String>) {
            val authorities = givenAuthorities.map(::SimpleGrantedAuthority)
            val current = authentication()
            val principal = current?.principal

            val authentication = if (principal is UserDetails) {
                UsernamePasswordAuthenticationToken(principal, current.credentials as? String, authorities)
            } else {
                AnonymousAuthenticationToken("anonymous", "anon", authorities)
            }

            SecurityContextHolder.getContext().authentication = authentication
        }
    }
}
