package net.lubble.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * JWTTool is a utility class for generating, verifying, and decoding JWT tokens.
 *
 * @param algorithm The algorithm used to sign and verify the JWT tokens.
 * @param issuer The issuer of the JWT tokens.
 * @param expiration The expiration time of the JWT tokens in milliseconds.
 * @param audience The audience of the JWT tokens.
 * @constructor Creates a new JWTTool with the specified secret, issuer, expiration, and audience.
 */
class JWTTool(
    private val algorithm: Algorithm,
    private var issuer: String,
    private var expiration: Long,
    private var audience: Array<String>,
) {
    constructor(secret: String) : this(Algorithm.HMAC512(secret), "", 0, arrayOf())

    constructor(algorithm: Algorithm) : this(algorithm, "", 0, arrayOf())

    // A verifier is thread-safe once built, so one is kept until issuer or audience change
    // instead of building a new one for every token.
    @Volatile
    private var verifier: JWTVerifier? = null

    private fun verifier(): JWTVerifier = verifier ?: JWT.require(algorithm)
        .withIssuer(issuer)
        // An empty audience means "not restricted". Passing it on would make the verifier demand
        // an `aud` claim that tokens generated without an audience do not carry.
        .apply { if (audience.isNotEmpty()) withAudience(*audience) }
        .build()
        .also { verifier = it }

    /** Verifies signature, issuer, audience and expiry; throws when any of them does not hold. */
    private fun verified(token: String): DecodedJWT = verifier().verify(token)

    /**
     * Generates a JWT with the specified subject, expiration time, and claims.
     *
     * @param subject The subject of the JWT.
     * @param expiration The expiration time of the JWT in milliseconds.
     * @param claims A map of claims to include in the JWT.
     * @return The generated JWT.
     */
    @Suppress("UNCHECKED_CAST")
    fun generate(subject: String, expiration: Long, claims: Map<String, Any>): String {
        val iat = Instant.now()
        val eat = iat.plusMillis(expiration)
        val jwt = JWT.create()
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(*audience)
            .withIssuedAt(iat)
            .withExpiresAt(eat)
        claims.forEach { (key, value) ->
            when (value) {
                is String -> jwt.withClaim(key, value)
                is Int -> jwt.withClaim(key, value)
                is Long -> jwt.withClaim(key, value)
                is Double -> jwt.withClaim(key, value)
                is Boolean -> jwt.withClaim(key, value)
                is Date -> jwt.withClaim(key, value)
                is Collection<*> -> jwt.withClaim(key, value.toMutableList())
                is Map<*, *> -> jwt.withClaim(key, value as Map<String, Any>)
            }
        }
        return jwt.sign(algorithm)
    }

    /**
     * Generates a JWT with the specified subject and claims.
     *
     * @param subject The subject of the JWT.
     * @param claims A map of claims to include in the JWT.
     * @return The generated JWT.
     */
    fun generate(subject: String, claims: Map<String, Any>): String {
        return generate(subject, expiration, claims)
    }

    /**
     * Verifies a JWT.
     *
     * @param token The JWT to verify.
     * @return True if the JWT is valid and not expired, false otherwise.
     *         Tokens that carry no expiry at all are rejected.
     */
    fun verify(token: String): Boolean {
        return try {
            verified(token).expiresAtAsInstant != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verifies a JWT and returns its claims as a map.
     *
     * @param token The JWT to decode.
     * @return A map of the claims in the JWT.
     */
    fun decode(token: String): Map<String, Claim> {
        return verified(token).claims
    }

    /**
     * Verifies a JWT and returns the value of a specific claim.
     *
     * @param token The JWT to decode.
     * @param key The key of the claim to return.
     * @return The value of the claim, or null if the claim is not present.
     */
    fun decode(token: String, key: String): Claim? {
        return verified(token).claims[key]
    }

    /**
     * Verifies a JWT and returns its subject.
     *
     * @param token The JWT to get the subject from.
     * @return The subject of the JWT.
     */
    fun subject(token: String): String {
        return verified(token).subject
    }

    fun audience(audience: Array<String>): JWTTool {
        this.audience = audience
        this.verifier = null
        return this
    }

    fun expiration(expiration: Long): JWTTool {
        this.expiration = expiration
        return this
    }

    fun expiration(expiration: Duration): JWTTool {
        this.expiration = expiration.toMillis()
        return this
    }

    fun issuer(issuer: String): JWTTool {
        this.issuer = issuer
        this.verifier = null
        return this
    }

    companion object {

        /**
         * Decodes a JWT and returns its claims as a map.
         *
         * WARNING: the static `decode`/`subject` helpers do NOT verify the signature. Never base an
         * authentication or authorization decision on their result; use a [JWTTool] instance for that.
         *
         * @param token The JWT to decode.
         * @return A map of the claims in the JWT.
         */
        fun decode(token: String): Map<String, Claim> {
            return JWT.decode(token).claims
        }

        /**
         * Decodes a JWT WITHOUT verifying it and returns the value of a specific claim.
         *
         * @param token The JWT to decode.
         * @param key The key of the claim to return.
         * @return The value of the claim, or null if the claim is not present.
         */
        fun decode(token: String, key: String): Claim? {
            return JWT.decode(token).claims[key]
        }

        /**
         * Returns the subject of a JWT WITHOUT verifying it.
         *
         * @param token The JWT to get the subject from.
         * @return The subject of the JWT.
         */
        fun subject(token: String): String {
            return JWT.decode(token).subject
        }

        /**
         * Verifies a JWT.
         *
         * @param token The JWT to verify.
         * @param algorithm The algorithm used to verify the JWT.
         * @return True if the JWT is valid and not expired, false otherwise.
         */
        fun verify(token: String, algorithm: Algorithm): Boolean {
            return try {
                JWT.require(algorithm).build().verify(token).expiresAtAsInstant != null
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Verifies a JWT.
         *
         * @param token The JWT to verify.
         * @param audience The audience of the JWT.
         * @param algorithm The algorithm used to verify the JWT.
         * @return True if the JWT is valid and not expired, false otherwise.
         */
        fun verify(token: String, audience: Array<String>, algorithm: Algorithm): Boolean {
            return try {
                JWT.require(algorithm).withAudience(*audience).build().verify(token).expiresAtAsInstant != null
            } catch (_: Exception) {
                false
            }
        }
    }
}
