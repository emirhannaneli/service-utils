package net.lubble.util.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import net.lubble.util.AppContextUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans
import org.springframework.context.annotation.Configuration
import java.net.http.HttpClient

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

    @Bean
    open fun http(): HttpClient {
        return HttpClient.newHttpClient()
    }
}
