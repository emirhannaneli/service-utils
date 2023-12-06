package net.lubble.util.config.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.rsocket.RSocket
import net.lubble.util.config.lubble.LubbleConfig
import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils
import reactor.util.retry.Retry
import java.time.Duration

@Configuration("lubbleRSocketConfig")
@ConditionalOnClass(RSocket::class)
open class RSocketConfig(val mapper: ObjectMapper, val config: LubbleConfig) {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
    init {
        log.info("Lubble Utils RSocketConfig initialized.")
    }
    fun rSocketRequesterBuilder(): RSocketRequester.Builder {
        val strategies = RSocketStrategies.builder()
            .decoder(Jackson2JsonDecoder(mapper))
            .encoder(Jackson2JsonEncoder(mapper))
            .dataBufferFactory(DefaultDataBufferFactory())
            .build()

        return RSocketRequester.builder()
            .rsocketConnector { it.reconnect(Retry.fixedDelay(3, Duration.ofSeconds(2))) }
            .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
            .rsocketStrategies(strategies)
    }

    @Bean("rSocketPhoneClient")
    @ConditionalOnProperty(prefix = "lubble.services.phone-service", name = ["host"])
    open fun rSocketPhoneClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.services.phoneService.host!!, config.services.phoneService.port!!)
    }

    @Bean("rSocketUserClient")
    @ConditionalOnProperty(prefix = "lubble.services.user-service", name = ["host"])
    open fun rSocketUserClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.services.userService.host!!, config.services.userService.port!!)
    }

    @Bean("rSocketAddressClient")
    @ConditionalOnProperty(prefix = "lubble.services.address-service", name = ["host"])
    open fun rSocketAddressClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.services.addressService.host!!, config.services.addressService.port!!)
    }

    @Bean("rSocketContentClient")
    @ConditionalOnProperty(prefix = "lubble.services.content-service", name = ["host"])
    open fun rSocketContentClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.services.contentService.host!!, config.services.contentService.port!!)
    }

    @Bean("rSocketMailClient")
    @ConditionalOnProperty(prefix = "lubble.services.mail-service", name = ["host"])
    open fun rSocketMailClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.services.mailService.host!!, config.services.mailService.port!!)
    }
}

enum class RSocketService(val beanName: String) {
    PHONE("rSocketPhoneClient"),
    USER("rSocketUserClient"),
    ADDRESS("rSocketAddressClient"),
    CONTENT("rSocketContentClient"),
    MAIL("rSocketMailClient");
}