package net.lubble.util.config.lubble

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
class LubbleConfig {
    var exceptionHandling: Boolean = true
    lateinit var security: Security
    lateinit var oauth2: OAuth2

    @ConfigurationProperties(prefix = "lubble.oauth2")
    class OAuth2 {
        lateinit var google: OAuth2Credentials
    }

    class OAuth2Credentials {
        lateinit var clientId: String
        lateinit var clientSecret: String
        lateinit var issuers: List<String>
    }

    @ConfigurationProperties(prefix = "lubble.security")
    class Security {
        lateinit var cookie: Cookie

        class Cookie {
            lateinit var prefix: String
        }
    }
}