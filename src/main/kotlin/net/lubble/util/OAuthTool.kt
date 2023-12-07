package net.lubble.util

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import net.lubble.util.config.lubble.LubbleConfig
import net.lubble.util.config.utils.EnableLubbleUtils
import net.lubble.util.exception.WrongCredentials
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "lubble.oauth2", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OAuthTool(val config: LubbleConfig) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
    init {
        log.info("Lubble Utils OAuthTool initialized.")
    }
    /**
     * Retrieves the user data from Google.
     * @param credentials The credentials to verify.
     * @return The user data. The map contains the following keys:
     * - userId: String?
     * - email: String?
     * - emailVerified: Boolean?
     * - name: String?
     * - pictureUrl: String?
     * - locale: String?
     * - familyName: String?
     * - givenName: String?
     * @throws WrongCredentials If the credentials are invalid.
     */
    fun retrieveGoogleData(credentials: String): Map<String, Any> {
        val factory = GsonFactory.getDefaultInstance()
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), factory)
            .setAudience(listOf(config.oauth2.google.clientId))
            .setIssuers(config.oauth2.google.issuers)
            .build()

        val idToken = verifier.verify(credentials) ?: throw WrongCredentials()

        val payload = idToken.payload
        val userId = payload.subject
        val email = payload.email
        val emailVerified = payload.emailVerified
        val name = payload["name"] as String
        val pictureUrl = payload["picture"] as String
        val locale = payload["locale"] as String
        val familyName = payload["family_name"] as String
        val givenName = payload["given_name"] as String

        return mapOf(
            "userId" to userId,
            "email" to email,
            "emailVerified" to emailVerified,
            "name" to name,
            "pictureUrl" to pictureUrl,
            "locale" to locale,
            "familyName" to familyName,
            "givenName" to givenName
        )
    }
}