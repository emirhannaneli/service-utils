package net.lubble.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Claim
import org.joda.time.DateTime
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
     * Checks if a JWT is expired.
     *
     * @param token The JWT to check.
     * @return True if the JWT is expired, false otherwise.
     */
    private fun isExpired(token: String): Boolean {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(*audience)
                .build()
                .verify(token)
                .expiresAt
                .before(DateTime.now().toDate())
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Verifies a JWT.
     *
     * @param token The JWT to verify.
     * @return True if the JWT is valid and not expired, false otherwise.
     */
    fun verify(token: String): Boolean {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(*audience)
                .build()
                .verify(token)
            !isExpired(token)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Decodes a JWT and returns its claims as a map.
     *
     * @param token The JWT to decode.
     * @return A map of the claims in the JWT.
     */
    fun decode(token: String): Map<String, Claim> {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims
            .mapValues { it.value }
    }

    /**
     * Decodes a JWT and returns the value of a specific claim.
     *
     * @param token The JWT to decode.
     * @param key The key of the claim to return.
     * @return The value of the claim, or an empty string if the claim is not present.
     */
    fun decode(token: String, key: String): Claim? {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims[key]
    }

    /**
     * Returns the subject of a JWT.
     *
     * @param token The JWT to get the subject from.
     * @return The subject of the JWT.
     */
    fun subject(token: String): String {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .subject
    }

    fun audience(audience: Array<String>): JWTTool {
        this.audience = audience
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
        return this
    }

    companion object {

        /**
         * Decodes a JWT and returns its claims as a map.
         *
         * @param token The JWT to decode.
         * @return A map of the claims in the JWT.
         */
        fun decode(token: String): Map<String, Claim> {
            return JWT.decode(token)
                .claims
                .mapValues { it.value }
        }

        /**
         * Decodes a JWT and returns the value of a specific claim.
         *
         * @param token The JWT to decode.
         * @param key The key of the claim to return.
         * @return The value of the claim, or an empty string if the claim is not present.
         */
        fun decode(token: String, key: String): Claim? {
            return JWT.decode(token)
                .claims[key]
        }

        /**
         * Returns the subject of a JWT.
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
                val expired = JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .expiresAt
                    .before(DateTime.now().toDate())
                !expired
            } catch (e: Exception) {
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
                val expired = JWT.require(algorithm)
                    .withAudience(*audience)
                    .build()
                    .verify(token)
                    .expiresAt
                    .before(DateTime.now().toDate())
                !expired
            } catch (e: Exception) {
                false
            }
        }
    }
}