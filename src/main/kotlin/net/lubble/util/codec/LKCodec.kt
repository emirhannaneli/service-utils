package net.lubble.util.codec

import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.stereotype.Component

@Component
@ConditionalOnClass(Codec::class)
class LKCodec : Codec<LK> {
    override fun encode(writer: BsonWriter, lk: LK, ctx: EncoderContext) {
        writer.writeString(lk.toString())
    }

    override fun getEncoderClass(): Class<LK> {
        return LK::class.java
    }

    override fun decode(reader: BsonReader, ctx: DecoderContext): LK {
        return LK(reader.readString())
    }

    @Bean
    fun lkConversions(): MongoCustomConversions {
        return MongoCustomConversions(listOf(LKToStringConverter.Serializer(), LKToStringConverter.Deserializer()))
    }
}