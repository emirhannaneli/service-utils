package net.lubble.util.config.lubble

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
class LubbleConfig {
    var exceptionHandling: Boolean = true
    var services: Services? = null
    lateinit var security: Security
    lateinit var oauth2: OAuth2

    @ConfigurationProperties(prefix = "lubble.services")
    class Services {
        lateinit var contentService: Service
        lateinit var phoneService: Service
        lateinit var userService: Service
        lateinit var addressService: Service
        lateinit var mailService: Service
    }

    class Service {
        lateinit var host: String
        lateinit var port: String
        lateinit var username: String
        lateinit var password: String
    }

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