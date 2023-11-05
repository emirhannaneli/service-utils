package net.lubble.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.joda.time.DateTime

class JWTTool(
    private val secret: String,
    private val issuer: String,
    private val expiration: Long,
    private val audience: Array<String>,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun generate(subject: String, claims: Array<String>): String {
        return JWT.create()
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(*audience)
            .withArrayClaim("claims", claims)
            .withExpiresAt(DateTime.now().plus(expiration).toDate())
            .sign(algorithm)
    }

    fun generate(subject: String, expiration: Long, claims: Array<String>): String {
        return JWT.create()
            .withSubject(subject)
            .withIssuer(issuer)
            .withAudience(*audience)
            .withArrayClaim("claims", claims)
            .withExpiresAt(DateTime.now().plus(expiration).toDate())
            .sign(algorithm)
    }

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

    fun decode(token: String): Map<String, Any> {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims
            .mapValues { it.value.asString() }
    }

    fun decode(token: String, key: String): String {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .claims[key]?.asString() ?: ""
    }

    fun subject(token: String): String {
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(*audience)
            .build()
            .verify(token)
            .subject
    }
}