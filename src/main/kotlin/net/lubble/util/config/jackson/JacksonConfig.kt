package net.lubble.util.config.jackson

import net.lubble.util.dto.RBase
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.BeanDescription
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.SerializationConfig
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.BeanPropertyWriter
import tools.jackson.databind.ser.ValueSerializerModifier

@Configuration
@ConditionalOnClass(JacksonModule::class)
open class JacksonConfig {

    @Bean
    open fun customOrderModule(): SimpleModule {
        val module = SimpleModule()

        val modifier = object : ValueSerializerModifier() {
            override fun orderProperties(
                config: SerializationConfig,
                beanDesc: BeanDescription.Supplier,
                beanProperties: List<BeanPropertyWriter>
            ): List<BeanPropertyWriter?>? {
                if (RBase::class.java.isAssignableFrom(beanDesc.beanClass)) {

                    val topProps = beanProperties.filter { it.name == "pk" || it.name == "sk" }

                    val bottomNames = listOf("updatedBy", "updatedAt", "createdBy", "createdAt")
                    val bottomProps = beanProperties.filter { it.name in bottomNames }

                    val middleProps = beanProperties.filterNot { prop ->
                        prop.name in topProps.map { it.name } || prop.name in bottomProps.map { it.name }
                    }

                    return (topProps + middleProps + bottomProps).toMutableList()
                }

                return beanProperties
            }
        }

        module.setSerializerModifier(modifier)

        return module
    }
}