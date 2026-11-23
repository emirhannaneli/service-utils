package net.lubble.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User

class AuthToolTest {
    private val user = User("ada", "secret", listOf(SimpleGrantedAuthority("ROLE_USER")))

    private fun login() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(user, "credentials", user.authorities)
    }

    @AfterEach
    fun clear() = SecurityContextHolder.clearContext()

    @Test
    fun `nothing is exposed without an authentication`() {
        assertThat(AuthTool.principal<Any>()).isNull()
        assertThat(AuthTool.credentials()).isNull()
        assertThat(AuthTool.isAuthenticated()).isFalse()
        assertThat(AuthTool.isAnonymous()).isTrue()
    }

    @Test
    fun `exposes principal and credentials of the logged in user`() {
        login()

        assertThat(AuthTool.principal<User>()).isSameAs(user)
        assertThat(AuthTool.credentials()).isEqualTo("credentials")
        assertThat(AuthTool.isAuthenticated()).isTrue()
        assertThat(AuthTool.isAnonymous()).isFalse()
    }

    @Test
    fun `a principal that is not UserDetails does not count as authenticated`() {
        SecurityContextHolder.getContext().authentication =
            AnonymousAuthenticationToken("key", "anonymousUser", listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS")))

        assertThat(AuthTool.isAuthenticated()).isFalse()
    }

    @Test
    fun `authorize replaces the authorities of a logged in user`() {
        login()

        AuthTool.authorize("ROLE_ADMIN", "ROLE_AUDITOR")

        val authentication = SecurityContextHolder.getContext().authentication!!
        assertThat(authentication.principal).isSameAs(user)
        assertThat(authentication.authorities.map { it.authority }).containsExactly("ROLE_ADMIN", "ROLE_AUDITOR")
        // Authorities must be well behaved values: usable in sets and comparable.
        assertThat(authentication.authorities.toSet()).contains(SimpleGrantedAuthority("ROLE_ADMIN"))
    }

    @Test
    fun `authorize without a user creates an anonymous authentication`() {
        AuthTool.authorize("ROLE_GUEST")

        val authentication = SecurityContextHolder.getContext().authentication
        assertThat(authentication).isInstanceOf(AnonymousAuthenticationToken::class.java)
        assertThat(authentication!!.authorities.map { it.authority }).containsExactly("ROLE_GUEST")
    }

    @Test
    fun `authorize without authorities and without a user leaves the context untouched`() {
        AuthTool.authorize()

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }
}
