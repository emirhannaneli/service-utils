package net.lubble.util

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails

class AuthTool {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> principal(): T? {
            if (isAnonymous()) return null
            return SecurityContextHolder.getContext().authentication.principal as T
        }

        fun credentials(): String? {
            if (isAnonymous()) return null;
            return SecurityContextHolder.getContext().authentication.credentials as String
        }

        fun isAuthenticated(): Boolean {
            return SecurityContextHolder.getContext().authentication != null
                    && SecurityContextHolder.getContext().authentication.principal != null
                    && SecurityContextHolder.getContext().authentication.principal is UserDetails
        }

        fun isAnonymous(): Boolean {
            return SecurityContextHolder.getContext().authentication == null
                    || SecurityContextHolder.getContext().authentication.principal == null
                    || SecurityContextHolder.getContext().authentication.principal !is UserDetails
        }

        fun authorize(vararg givenAuthorities: String) {
            val authorities = ArrayList<GrantedAuthority>()
            val user: UserDetails? = principal()
            val credentials: String? = credentials()

            for (authority in givenAuthorities)
                authorities.add(GrantedAuthority { authority })

            if (isAuthenticated()) {
                val authentication = UsernamePasswordAuthenticationToken(user, credentials, authorities)
                SecurityContextHolder.getContext().authentication = authentication
                return
            }

            val authentication = UsernamePasswordAuthenticationToken(null, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }
    }
}