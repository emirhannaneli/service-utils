package net.lubble.util.converter

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import net.lubble.util.LK
import org.springframework.data.elasticsearch.core.mapping.PropertyValueConverter
import java.io.IOException

@Converter(autoApply = true)
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

    class Serializer : JsonSerializer<LK>(), org.springframework.core.convert.converter.Converter<LK, String> {
        @Throws(IOException::class)
        override fun serialize(value: LK, jgen: JsonGenerator, serializers: SerializerProvider) {
            jgen.writeString(value.toString())
        }

        override fun convert(source: LK): String {
            return source.toString()
        }
    }

    class Deserializer : JsonDeserializer<LK>(), org.springframework.core.convert.converter.Converter<String, LK> {
        @Throws(IOException::class, JsonProcessingException::class)
        override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): LK {
            val node: JsonNode = jsonParser.codec.readTree(jsonParser)
            return LK(node.asText())
        }

        override fun convert(source: String): LK {
            return LK(source)
        }
    }
}