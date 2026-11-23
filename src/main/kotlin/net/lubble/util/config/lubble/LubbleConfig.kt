package net.lubble.util.config.lubble

import net.lubble.util.spec.PaginationSpec
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
class LubbleConfig {
    var exceptionHandling: Boolean = true

    /**
     * Whether unexpected (HTTP 500) errors expose the exception class and message to the client.
     * Off by default: those messages routinely carry SQL, class names and other internals.
     */
    var exposeExceptionDetails: Boolean = false

    var pagination: Pagination = Pagination()
    var security: Security = Security()
    var oauth2: OAuth2 = OAuth2()

    class Pagination {
        /** Upper bound for the `size` request parameter; larger values are reduced to it. */
        var maxSize: Int = PaginationSpec.DEFAULT_MAX_SIZE
    }

    @ConfigurationProperties(prefix = "lubble.oauth2")
    class OAuth2 {
        var google: OAuth2Credentials = OAuth2Credentials()
    }

    class OAuth2Credentials {
        var clientId: String = ""
        var clientSecret: String = ""
        var issuers: List<String> = listOf("accounts.google.com", "https://accounts.google.com")
    }

    @ConfigurationProperties(prefix = "lubble.security")
    class Security {
        var cookie: Cookie = Cookie()

        class Cookie {
            var prefix: String = ""
        }
    }
}
