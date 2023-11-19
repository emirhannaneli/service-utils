package net.lubble.util.config.encoder

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@ConditionalOnClass(PasswordEncoder::class)
open class EncoderConfig {
    @Bean
    open fun encoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}