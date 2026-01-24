package net.lubble.util.converter

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import jakarta.persistence.AttributeConverter
import net.lubble.util.LK
import org.springframework.core.convert.converter.Converter
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer
import jakarta.persistence.Converter as JPAConverter

@JPAConverter(autoApply = true)
class LKToStringConverter : AttributeConverter<LK, String>, PropertyValueConverter {
    override fun convertToDatabaseColumn(value: LK): String {
        return value.toString()
    }

    override fun convertToEntityAttribute(value: String): LK {
        return LK(value)
    }

    override fun write(value: Any): Any {
        if (value !is LK) return value
        return value.value
    }

    override fun read(value: Any): Any {
        if (value !is String) return value
        return LK(value)
    }

    class Serializer : ValueSerializer<LK>(), Converter<LK, String> {
        override fun convert(source: LK): String {
            return source.toString()
        }

        override fun serialize(
            value: LK,
            gen: JsonGenerator,
            ctxt: SerializationContext
        ) {
            gen.writeString(value.toString())
        }

        class Json : JsonSerializer<LK>() {

            override fun serialize(
                value: LK,
                gen: com.fasterxml.jackson.core.JsonGenerator,
                serializers: SerializerProvider
            ) {
                gen.writeString(value.toString())
            }
        }
    }

    class Deserializer : ValueDeserializer<LK>(), Converter<String, LK> {
        override fun deserialize(
            p: JsonParser,
            ctxt: DeserializationContext
        ): LK {
            val value = p.valueAsString
            return LK(value)
        }

        override fun convert(source: String): LK {
            return LK(source)
        }

        class Json : com.fasterxml.jackson.databind.JsonDeserializer<LK>() {
            override fun deserialize(
                p: com.fasterxml.jackson.core.JsonParser,
                ctxt: com.fasterxml.jackson.databind.DeserializationContext
            ): LK {
                val value = p.valueAsString
                return LK(value)
            }
        }
    }
}