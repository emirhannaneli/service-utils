package net.lubble.util.config.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PostConstruct
import net.lubble.util.AppContextUtil
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.*
import org.springframework.context.event.EventListener
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
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

    @PostConstruct
    fun init() {
        AppContextUtil.initialize(context)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        log.info("Lubble Utils initialized with <3")
    }

    @Bean
    open fun mapper(): ObjectMapper {
        val mapper = ObjectMapper()

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        mapper.registerKotlinModule()

        val lkModule = SimpleModule()
        lkModule.addSerializer(LK::class.java, LKToStringConverter.Serializer())
        lkModule.addDeserializer(LK::class.java, LKToStringConverter.Deserializer())

        mapper.registerModule(lkModule)

        return mapper
    }

    @Bean
    open fun http(): HttpClient {
        return HttpClient.newHttpClient()
    }

}

private const val COMPONENT_SCAN = "net.lubble.util"
private const val CONFIGURATION_PROPERTIES_SCAN = "net.lubble.util.config"
