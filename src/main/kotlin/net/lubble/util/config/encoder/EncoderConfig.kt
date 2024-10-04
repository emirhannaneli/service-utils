package net.lubble.util.config.encoder

import net.lubble.util.config.utils.EnableLubbleUtils
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@ConditionalOnClass(PasswordEncoder::class)
open class EncoderConfig {
    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)
    @Bean
    open fun encoder(): PasswordEncoder {
        log.info("Lubble Utils Encoder initialized.")
        return BCryptPasswordEncoder()
    }
}