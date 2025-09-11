package net.lubble.util

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails

/**
 * AuthTool is a utility class that provides methods for handling authentication and authorization.
 * It uses Spring Security's SecurityContextHolder to access the authentication and authorization information.
 */
class AuthTool {
    companion object {
        /**
         * Retrieves the principal (usually the user) from the current authentication context.
         *
         * @return The principal if the user is authenticated, null otherwise.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> principal(): T? {
            if (isAnonymous()) return null
            return try {
                SecurityContextHolder.getContext().authentication.principal as T
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Retrieves the credentials from the current authentication context.
         *
         * @return The credentials if the user is authenticated, null otherwise.
         */
        fun credentials(): String? {
            if (isAnonymous()) return null
            return SecurityContextHolder.getContext().authentication.credentials as String
        }

        /**
         * Checks if the user is authenticated.
         *
         * @return True if the user is authenticated, false otherwise.
         */
        fun isAuthenticated(): Boolean {
            return SecurityContextHolder.getContext().authentication != null
                    && SecurityContextHolder.getContext().authentication.principal != null
                    && SecurityContextHolder.getContext().authentication.principal is UserDetails
        }

        /**
         * Checks if the user is anonymous.
         *
         * @return True if the user is anonymous, false otherwise.
         */
        fun isAnonymous(): Boolean {
            return SecurityContextHolder.getContext().authentication == null
                    || SecurityContextHolder.getContext().authentication.principal == null
        }

        /**
         * Authorizes the user with the given authorities.
         *
         * @param givenAuthorities The authorities to grant to the user.
         */
        fun authorize(vararg givenAuthorities: String) {
            val authorities = givenAuthorities.map { GrantedAuthority { it } }
            val authentication = if (isAuthenticated()) {
                UsernamePasswordAuthenticationToken(principal<UserDetails>(), credentials(), authorities)
            } else {
                UsernamePasswordAuthenticationToken(null, null, authorities)
            }
            SecurityContextHolder.getContext().authentication = authentication
        }
    }
}