package net.lubble.util.config.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import jakarta.annotation.PostConstruct
import net.lubble.util.AppContextUtil
import net.lubble.util.LID
import net.lubble.util.converter.LIDStringConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.*
import org.springframework.context.event.EventListener
import java.net.http.HttpClient

@Configuration
@ComponentScans(
    ComponentScan(COMPONENT_SCAN),
)
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan(CONFIGURATION_PROPERTIES_SCAN)
open class EnableLubbleUtilsConfig {

    @Autowired
    private lateinit var context: ApplicationContext

    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun init() {
        AppContextUtil.initialize(context)

        log.info("Lubble Utils initialized with <3")
    }

    @Bean
    open fun mapper(): ObjectMapper {
        val mapper = ObjectMapper()

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

        val lidConverter = LIDStringConverter()
        val lidModule = SimpleModule()
        lidModule.addSerializer(LID::class.java, lidConverter)

        mapper.registerModule(lidModule)

        return mapper
    }

    @Bean
    open fun http(): HttpClient {
        return HttpClient.newHttpClient()
    }
}

private const val COMPONENT_SCAN = "net.lubble.util"
private const val CONFIGURATION_PROPERTIES_SCAN = "net.lubble.util.config.lubble"
