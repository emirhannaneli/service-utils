package net.lubble.util.config.utils

import net.lubble.util.AppContextUtil
import net.lubble.util.LK
import net.lubble.util.SpringDetectionUtil
import net.lubble.util.converter.LKToStringConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.*
import org.springframework.context.event.EventListener
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.mongodb.config.EnableMongoAuditing
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.blackbird.BlackbirdModule
import tools.jackson.module.kotlin.KotlinModule
import java.net.http.HttpClient

@Configuration
@ComponentScans(
    ComponentScan(COMPONENT_SCAN),
)
@EnableJpaAuditing
@EnableMongoAuditing
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan(CONFIGURATION_PROPERTIES_SCAN)
open class EnableLubbleUtilsConfig : ApplicationContextAware {

    private val log = LoggerFactory.getLogger(EnableLubbleUtils::class.java)

    init {
        if (!SpringDetectionUtil.isSpringAvailable()) {
            log.warn(
                "@EnableLubbleUtils annotation is used but Spring Framework is not available in classpath. " +
                "Spring features will not be available. Please ensure Spring Boot is in your dependencies."
            )
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        if (SpringDetectionUtil.isSpringAvailable()) {
            log.info("Lubble Utils initialized with <3")
        } else {
            log.warn("Lubble Utils attempted to initialize but Spring Framework is not available.")
        }
    }

    @Bean
    open fun mapper(): ObjectMapper {
        return JsonMapper.builderWithJackson2Defaults().apply {
            configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
            configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true)

            addModule(
                KotlinModule.Builder()
                    .withReflectionCacheSize(512)
                    .build()
            )

            addModule(BlackbirdModule())

            val lkModule = SimpleModule()
            lkModule.addSerializer(LK::class.java, LKToStringConverter.Serializer())
            lkModule.addDeserializer(LK::class.java, LKToStringConverter.Deserializer())
            addModule(lkModule)

        }.build()
    }

    @Bean
    open fun http(): HttpClient {
        return HttpClient.newHttpClient()
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        AppContextUtil.initialize(applicationContext)
    }

}

private const val COMPONENT_SCAN = "net.lubble.util"
private const val CONFIGURATION_PROPERTIES_SCAN = "net.lubble.util.config"
