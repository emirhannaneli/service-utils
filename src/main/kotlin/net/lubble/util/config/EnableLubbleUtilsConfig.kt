package net.lubble.util.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import net.lubble.util.AppContextUtil
import net.lubble.util.handler.RestExceptionHandler
import net.lubble.util.spec.CookieUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.*
import org.springframework.stereotype.Component
@Configuration
@ComponentScans(
    ComponentScan("net.lubble.util"),
)
open class EnableLubbleUtilsConfig {

    @Autowired
    private lateinit var context: ApplicationContext

    private val log = LoggerFactory.getLogger(EnableLubbleUtilsConfig::class.java)

    @PostConstruct
    fun init() {
        AppContextUtil.initialize(context)

        log.info("Lubble Utils initialized.")
    }

    @Bean
    open fun mapper(): ObjectMapper {
        val mapper = ObjectMapper()

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

        return mapper
    }
}
