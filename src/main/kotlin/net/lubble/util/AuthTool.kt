package net.lubble.util

import org.slf4j.LoggerFactory

/**
 * AuthTool is a utility class that provides methods for handling authentication and authorization.
 * It uses Spring Security's SecurityContextHolder to access the authentication and authorization information.
 * 
 * When Spring Security is not available, all methods return null or false.
 */
class AuthTool {
    companion object {
        private val log = LoggerFactory.getLogger(AuthTool::class.java)
        private var securityContextHolderClass: Class<*>? = null
        private var userDetailsClass: Class<*>? = null
        private var anonymousAuthTokenClass: Class<*>? = null
        private var usernamePasswordAuthTokenClass: Class<*>? = null
        private var grantedAuthorityClass: Class<*>? = null

        init {
            if (SpringDetectionUtil.isSpringSecurityAvailable()) {
                try {
                    securityContextHolderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder")
                    userDetailsClass = Class.forName("org.springframework.security.core.userdetails.UserDetails")
                    anonymousAuthTokenClass = Class.forName("org.springframework.security.authentication.AnonymousAuthenticationToken")
                    usernamePasswordAuthTokenClass = Class.forName("org.springframework.security.authentication.UsernamePasswordAuthenticationToken")
                    grantedAuthorityClass = Class.forName("org.springframework.security.core.GrantedAuthority")
                } catch (e: Exception) {
                    log.debug("Failed to load Spring Security classes: ${e.message}")
                }
            } else {
                log.debug("Spring Security is not available. AuthTool methods will return null/false.")
            }
        }

        /**
         * Retrieves the principal (usually the user) from the current authentication context.
         *
         * @return The principal if the user is authenticated and Spring Security is available, null otherwise.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> principal(): T? {
            if (!SpringDetectionUtil.isSpringSecurityAvailable() || securityContextHolderClass == null) {
                return null
            }
            if (isAnonymous()) return null
            return try {
                val getContextMethod = securityContextHolderClass!!.getMethod("getContext")
                val context = getContextMethod.invoke(null)
                val getAuthenticationMethod = context.javaClass.getMethod("getAuthentication")
                val authentication = getAuthenticationMethod.invoke(context)
                if (authentication == null) return null
                val getPrincipalMethod = authentication.javaClass.getMethod("getPrincipal")
                getPrincipalMethod.invoke(authentication) as? T
            } catch (e: Exception) {
                log.debug("Error retrieving principal: ${e.message}")
                null
            }
        }

        /**
         * Retrieves the credentials from the current authentication context.
         *
         * @return The credentials if the user is authenticated and Spring Security is available, null otherwise.
         */
        fun credentials(): String? {
            if (!SpringDetectionUtil.isSpringSecurityAvailable() || securityContextHolderClass == null) {
                return null
            }
            if (isAnonymous()) return null
            return try {
                val getContextMethod = securityContextHolderClass!!.getMethod("getContext")
                val context = getContextMethod.invoke(null)
                val getAuthenticationMethod = context.javaClass.getMethod("getAuthentication")
                val authentication = getAuthenticationMethod.invoke(context)
                if (authentication == null) return null
                val getCredentialsMethod = authentication.javaClass.getMethod("getCredentials")
                getCredentialsMethod.invoke(authentication) as? String
            } catch (e: Exception) {
                log.debug("Error retrieving credentials: ${e.message}")
                null
            }
        }

        /**
         * Checks if the user is authenticated.
         *
         * @return True if the user is authenticated and Spring Security is available, false otherwise.
         */
        fun isAuthenticated(): Boolean {
            if (!SpringDetectionUtil.isSpringSecurityAvailable() || securityContextHolderClass == null || userDetailsClass == null) {
                return false
            }
            return try {
                val getContextMethod = securityContextHolderClass!!.getMethod("getContext")
                val context = getContextMethod.invoke(null)
                val getAuthenticationMethod = context.javaClass.getMethod("getAuthentication")
                val authentication = getAuthenticationMethod.invoke(context)
                if (authentication == null) return false
                val getPrincipalMethod = authentication.javaClass.getMethod("getPrincipal")
                val principal = getPrincipalMethod.invoke(authentication)
                principal != null && userDetailsClass!!.isInstance(principal)
            } catch (e: Exception) {
                log.debug("Error checking authentication: ${e.message}")
                false
            }
        }

        /**
         * Checks if the user is anonymous.
         *
         * @return True if the user is anonymous or Spring Security is not available, false otherwise.
         */
        fun isAnonymous(): Boolean {
            if (!SpringDetectionUtil.isSpringSecurityAvailable() || securityContextHolderClass == null) {
                return true
            }
            return try {
                val getContextMethod = securityContextHolderClass!!.getMethod("getContext")
                val context = getContextMethod.invoke(null)
                val getAuthenticationMethod = context.javaClass.getMethod("getAuthentication")
                val authentication = getAuthenticationMethod.invoke(context)
                authentication == null || run {
                    val getPrincipalMethod = authentication.javaClass.getMethod("getPrincipal")
                    getPrincipalMethod.invoke(authentication) == null
                }
            } catch (e: Exception) {
                log.debug("Error checking anonymous status: ${e.message}")
                true
            }
        }

        /**
         * Authorizes the user with the given authorities.
         * This method only works when Spring Security is available.
         *
         * @param givenAuthorities The authorities to grant to the user.
         */
        fun authorize(vararg givenAuthorities: String) {
            if (!SpringDetectionUtil.isSpringSecurityAvailable() || 
                securityContextHolderClass == null || 
                grantedAuthorityClass == null ||
                anonymousAuthTokenClass == null ||
                usernamePasswordAuthTokenClass == null) {
                log.debug("Spring Security is not available. Cannot authorize user.")
                return
            }
            try {
                // Create GrantedAuthority instances using Proxy
                val authorities = givenAuthorities.map { authority ->
                    java.lang.reflect.Proxy.newProxyInstance(
                        grantedAuthorityClass!!.classLoader,
                        arrayOf(grantedAuthorityClass!!)
                    ) { _, method, _ ->
                        if (method.name == "getAuthority") authority else null
                    }
                }
                
                val getContextMethod = securityContextHolderClass!!.getMethod("getContext")
                val context = getContextMethod.invoke(null)
                val authenticationClass = Class.forName("org.springframework.security.core.Authentication")
                val setAuthenticationMethod = context.javaClass.getMethod("setAuthentication", authenticationClass)
                
                val authentication = if (isAuthenticated()) {
                    val principal = principal<Any>()
                    val credentials = credentials()
                    val constructor = usernamePasswordAuthTokenClass!!.getConstructor(
                        Any::class.java,
                        Any::class.java,
                        Collection::class.java
                    )
                    constructor.newInstance(principal, credentials, authorities)
                } else {
                    val constructor = anonymousAuthTokenClass!!.getConstructor(
                        String::class.java,
                        Any::class.java,
                        Collection::class.java
                    )
                    constructor.newInstance("anonymous", "anon", authorities)
                }
                
                setAuthenticationMethod.invoke(context, authentication)
            } catch (e: Exception) {
                log.warn("Failed to authorize user: ${e.message}", e)
            }
        }
    }
}