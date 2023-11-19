package net.lubble.util.config.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import net.lubble.util.config.lubble.LubbleConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.rsocket.server.RSocketServer
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

@Configuration
@ConditionalOnClass(RSocketServer::class)
open class RSocketConfig(val mapper: ObjectMapper, val config: LubbleConfig) {
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
    @ConditionalOnProperty(prefix = "lubble.phone-service", name = ["host"])
    fun rSocketPhoneClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.phoneService.host!!, config.phoneService.port!!)
    }

    @Bean("rSocketUserClient")
    @ConditionalOnProperty(prefix = "lubble.user-service", name = ["host"])
    fun rSocketUserClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.userService.host!!, config.userService.port!!)
    }

    @Bean("rSocketAddressClient")
    @ConditionalOnProperty(prefix = "lubble.address-service", name = ["host"])
    fun rSocketAddressClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.addressService.host!!, config.addressService.port!!)
    }

    @Bean("rSocketContentClient")
    @ConditionalOnProperty(prefix = "lubble.content-service", name = ["host"])
    fun rSocketContentClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.contentService.host!!, config.contentService.port!!)
    }

    @Bean("rSocketMailClient")
    @ConditionalOnProperty(prefix = "lubble.mail-service", name = ["host"])
    fun rSocketMailClient(): RSocketRequester {
        return rSocketRequesterBuilder()
            .tcp(config.mailService.host!!, config.mailService.port!!)
    }
}