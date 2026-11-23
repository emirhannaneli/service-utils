package net.lubble.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Date

class JWTToolTest {
    private val secret = "a-test-secret-that-is-long-enough-for-hmac-sha-512-signatures-0123456789"
    private val algorithm = Algorithm.HMAC512(secret)

    private fun tool() = JWTTool(secret).issuer("lubble").audience(arrayOf("web")).expiration(Duration.ofMinutes(5))

    @Test
    fun `generated token verifies and exposes subject and claims`() {
        val tool = tool()
        val token = tool.generate(
            "user-1",
            mapOf(
                "name" to "Ada", "age" to 36, "id" to 9_000_000_000L, "ratio" to 0.5, "admin" to true,
                "since" to Date(1_700_000_000_000), "roles" to listOf("a", "b"), "meta" to mapOf("k" to "v"),
            )
        )

        assertThat(tool.verify(token)).isTrue()
        assertThat(tool.subject(token)).isEqualTo("user-1")
        val claims = tool.decode(token)
        assertThat(claims["name"]!!.asString()).isEqualTo("Ada")
        assertThat(claims["age"]!!.asInt()).isEqualTo(36)
        assertThat(claims["id"]!!.asLong()).isEqualTo(9_000_000_000L)
        assertThat(claims["ratio"]!!.asDouble()).isEqualTo(0.5)
        assertThat(claims["admin"]!!.asBoolean()).isTrue()
        assertThat(claims["roles"]!!.asList(String::class.java)).containsExactly("a", "b")
        assertThat(claims["meta"]!!.asMap()).containsEntry("k", "v")
        assertThat(tool.decode(token, "name")!!.asString()).isEqualTo("Ada")
        assertThat(tool.decode(token, "missing")).isNull()
    }

    @Test
    fun `explicit expiration overrides the default one`() {
        val token = tool().generate("user-1", 60_000, emptyMap())

        val expiresAt = JWT.decode(token).expiresAtAsInstant
        assertThat(expiresAt).isBetween(Instant.now().plusSeconds(50), Instant.now().plusSeconds(70))
    }

    @Test
    fun `expired token is rejected`() {
        val tool = tool()
        val token = tool.generate("user-1", -60_000, emptyMap())

        assertThat(tool.verify(token)).isFalse()
        assertThatThrownBy { tool.subject(token) }.isInstanceOf(JWTVerificationException::class.java)
        assertThatThrownBy { tool.decode(token) }.isInstanceOf(JWTVerificationException::class.java)
    }

    @Test
    fun `token without expiry is rejected`() {
        val token = JWT.create().withSubject("user-1").withIssuer("lubble").withAudience("web").sign(algorithm)

        assertThat(tool().verify(token)).isFalse()
        assertThat(JWTTool.verify(token, algorithm)).isFalse()
    }

    @Test
    fun `token signed with another secret is rejected`() {
        val forged = JWTTool("another-secret-another-secret-another-secret-another-secret-0123456789")
            .issuer("lubble").audience(arrayOf("web")).generate("user-1", 60_000, emptyMap())

        assertThat(tool().verify(forged)).isFalse()
        assertThatThrownBy { tool().subject(forged) }.isInstanceOf(JWTVerificationException::class.java)
    }

    @Test
    fun `unsigned alg none token is rejected`() {
        val header = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"none","typ":"JWT"}""".toByteArray())
        val exp = Instant.now().plusSeconds(600).epochSecond
        val payload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"sub":"user-1","iss":"lubble","aud":"web","exp":$exp}""".toByteArray())

        assertThat(tool().verify("$header.$payload.")).isFalse()
    }

    @Test
    fun `tampered payload is rejected`() {
        val tool = tool()
        val parts = tool.generate("user-1", 60_000, mapOf("admin" to false)).split(".")
        val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1])).replace("\"admin\":false", "\"admin\":true")
        val tampered = parts[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray()) + "." + parts[2]

        assertThat(tool.verify(tampered)).isFalse()
    }

    @Test
    fun `issuer and audience are enforced and can be changed after first use`() {
        val token = tool().generate("user-1", 60_000, emptyMap())
        val verifier = tool()
        assertThat(verifier.verify(token)).isTrue()

        // The verifier is cached; changing issuer/audience must not keep using the old one.
        assertThat(verifier.issuer("someone-else").verify(token)).isFalse()
        assertThat(verifier.issuer("lubble").verify(token)).isTrue()
        assertThat(verifier.audience(arrayOf("mobile")).verify(token)).isFalse()
    }

    @Test
    fun `garbage input is rejected without throwing`() {
        assertThat(tool().verify("not-a-token")).isFalse()
        assertThat(tool().verify("")).isFalse()
        assertThat(JWTTool.verify("not-a-token", algorithm)).isFalse()
        assertThat(JWTTool.verify("not-a-token", arrayOf("web"), algorithm)).isFalse()
    }

    @Test
    fun `static verify checks signature expiry and audience`() {
        val token = tool().generate("user-1", 60_000, emptyMap())

        assertThat(JWTTool.verify(token, algorithm)).isTrue()
        assertThat(JWTTool.verify(token, arrayOf("web"), algorithm)).isTrue()
        assertThat(JWTTool.verify(token, arrayOf("mobile"), algorithm)).isFalse()
        assertThat(JWTTool.verify(token, Algorithm.HMAC512("x".repeat(64)))).isFalse()
        assertThat(JWTTool.verify(tool().generate("user-1", -1_000, emptyMap()), algorithm)).isFalse()
    }

    @Test
    fun `static decode reads claims without verifying`() {
        val token = tool().generate("user-1", 60_000, mapOf("name" to "Ada"))

        assertThat(JWTTool.subject(token)).isEqualTo("user-1")
        assertThat(JWTTool.decode(token)["name"]!!.asString()).isEqualTo("Ada")
        assertThat(JWTTool.decode(token, "name")!!.asString()).isEqualTo("Ada")
        assertThat(JWTTool.decode(token, "missing")).isNull()
    }

    @Test
    fun `algorithm constructor works with defaults`() {
        val tool = JWTTool(algorithm)
        val token = tool.generate("user-1", 60_000, emptyMap())

        assertThat(tool.verify(token)).isTrue()
    }
}
