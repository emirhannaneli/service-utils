package net.lubble.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.joda.time.DateTime
import java.time.Duration

/**
 * JWTTool is a utility class for generating, verifying, and decoding JWT tokens.
 *
 * @param secret The secret key used to sign and verify JWT tokens.
 * @param issuer The issuer of the JWT tokens.
 * @param expiration The expiration time of the JWT tokens in milliseconds.
 * @param audience The audience of the JWT tokens.
 * @constructor Creates a new JWTTool with the specified secret, issuer, expiration, and audience.
 */
class JWTTool(
    private val secret: String,
    private var issuer: String,
    private var expiration: Long,
    private var audience: Array<String>,
) {
    private val algorithm: Algorithm
        get() = Algorithm.HMAC256(secret)

    constructor(secret: String) : this(secret, "", 0, emptyArray())

    /**
     * Generates a JWT with the specified subject and claims.
     *
     * @param subject The subject of the JWT.
     * @param claims A map of claims to include in the JWT.
     * @return The generated JWT.
     */
    fun generate(subject: String, claims: Map<String, String>): String {
        val jwt = JWT.create()
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(*audience)
            .withExpiresAt(DateTime.now().plus(expiration).toDate())
        claims.forEach { (key, value) -> jwt.withClaim(key, value) }
        return jwt.sign(algorithm)
    }

    /**
     * Generates a JWT with the specified subject, expiration time, and claims.
     *
     * @param subject The subject of the JWT.
     * @param expiration The expiration time of the JWT in milliseconds.
     * @param claims A map of claims to include in the JWT.
     * @return The generated JWT.
     */
    fun generate(subject: String, expiration: Long, claims: Map<String, String>): String {
        val jwt = JWT.create()
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(*audience)
            .withExpiresAt(DateTime.now().plus(expiration).toDate())
        claims.forEach { (key, value) -> jwt.withClaim(key, value) }
        return jwt.sign(algorithm)
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
    fun decode(token: String): Map<String, Any> {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims
            .mapValues { it.value.asString() }
    }

    /**
     * Decodes a JWT and returns the value of a specific claim.
     *
     * @param token The JWT to decode.
     * @param key The key of the claim to return.
     * @return The value of the claim, or an empty string if the claim is not present.
     */
    fun decode(token: String, key: String): String {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims[key]?.asString() ?: ""
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
}