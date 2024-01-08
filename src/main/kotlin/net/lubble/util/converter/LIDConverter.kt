package net.lubble.util.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import net.lubble.util.LID

@Converter(autoApply = true)
class LIDConverter : AttributeConverter<LID, ByteArray> {
    override fun convertToDatabaseColumn(attribute: LID?): ByteArray {
        if (attribute != null) return attribute.toByteArray()
        throw Exception("LID cannot be null")
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): LID {
        if (dbData != null) return LID(dbData)
        throw Exception("LID cannot be null")
    }
}