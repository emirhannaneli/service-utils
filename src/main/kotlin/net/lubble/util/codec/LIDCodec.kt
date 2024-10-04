package net.lubble.util.codec

import net.lubble.util.LID
import org.bson.BsonBinary
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class LIDCodec : Codec<LID> {
    override fun encode(writer: BsonWriter?, value: LID?, encoderContext: EncoderContext?) {
        writer?.writeBinaryData(value?.toKey(), BsonBinary(value?.toByteArray()))
    }

    override fun getEncoderClass(): Class<LID> {
        return LID::class.java
    }

    override fun decode(reader: BsonReader?, decoderContext: DecoderContext?): LID {
        val bytes = reader?.readBinaryData()?.data
        return LID(bytes ?: ByteArray(0))
    }
}