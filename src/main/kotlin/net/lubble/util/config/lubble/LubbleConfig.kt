package net.lubble.util.config.lubble

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lubble")
open class LubbleConfig {
    var services: Services = Services()

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
}