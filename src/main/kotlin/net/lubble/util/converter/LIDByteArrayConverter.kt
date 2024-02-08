package net.lubble.util.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import net.lubble.util.LID

@Converter(autoApply = true)
class LIDByteArrayConverter : AttributeConverter<LID, ByteArray> {
    override fun convertToDatabaseColumn(attribute: LID?): ByteArray {
        return attribute?.toByteArray() ?: throw Exception("LID cannot be null")
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): LID {
        return dbData?.let { LID.fromByteArray(it) } ?: throw Exception("LID cannot be null")
    }
}