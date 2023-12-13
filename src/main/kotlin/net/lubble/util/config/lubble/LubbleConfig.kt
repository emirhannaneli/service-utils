package net.lubble.util.config.lubble

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
open class LubbleConfig {
    var security: Security = Security()
    var services: Services = Services()
    var oauth2: OAuth2 = OAuth2()

    open class Services {
        var contentService: Service = Service()
        var phoneService: Service = Service()
        var userService: Service = Service()
        var addressService: Service = Service()
        var mailService: Service = Service()
    }

    open class Service {
        var host: String? = null
        var port: Int? = null
        var username: String? = null
        var password: String? = null
    }

    open class OAuth2 {
        var google: OAuth2Credentials = OAuth2Credentials()
    }

    open class OAuth2Credentials {
        var clientId: String? = null
        var clientSecret: String? = null
        var issuers: List<String> = listOf()
    }

    open class Security {
        var cookie: Cookie = Cookie()
        open class Cookie {
            var prefix: String? = null
        }
    }
}