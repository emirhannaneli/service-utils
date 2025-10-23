package net.lubble.util.converter

import net.lubble.util.LK
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions
import java.time.Instant

@Configuration
@ConditionalOnClass(ElasticsearchCustomConversions::class)
open class ElasticConverter() {
    class LKToString : Converter<LK, String> {
        override fun convert(source: LK): String {
            return source.value
        }
    }

    class StringToLK : Converter<String, LK> {
        override fun convert(source: String): LK {
            return LK(source)
        }
    }

    class LongToInstantConverter : Converter<Long, Instant> {
        override fun convert(source: Long): Instant {
            return Instant.ofEpochMilli(source)
        }
    }

    class InstantToLongConverter : Converter<Instant, Long> {
        override fun convert(source: Instant): Long {
            return source.toEpochMilli()
        }
    }

    @Bean
    open fun conversions(): ElasticsearchCustomConversions {
        val converters = listOf(
            LKToString(),
            StringToLK(),
            LongToInstantConverter(),
            InstantToLongConverter(),
        )
        return ElasticsearchCustomConversions(converters)
    }
}