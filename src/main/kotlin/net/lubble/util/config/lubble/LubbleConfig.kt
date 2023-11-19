package net.lubble.util.config.lubble

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
class LubbleConfig {
    var contentService: Service = Service()
    var phoneService: Service = Service()
    var userService: Service = Service()
    var addressService: Service = Service()
    var mailService: Service = Service()

    class Service {
        var host: String? = null
        var port: Int? = null
        var username: String? = null
        var password: String? = null
    }
}