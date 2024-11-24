package net.lubble.util.config.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler

@Configuration
@ConditionalOnClass(DefaultMethodSecurityExpressionHandler::class)
open class SecurityConfig {
    @Bean
    open fun methodSecurityExpressionHandler(): MethodSecurityExpressionHandler {
        return DefaultMethodSecurityExpressionHandler()
    }
}