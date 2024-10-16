package net.lubble.util.config.logging

import org.springframework.context.annotation.Configuration


@Configuration
open class RequestLoggingConfig {
    /*@Bean
    open fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.isIncludeQueryString = true
        filter.isIncludePayload = true
        filter.setMaxPayloadLength(10000)
        filter.isIncludeHeaders = true
        filter.setAfterMessagePrefix("payload: ")
        return filter
    }*/
}