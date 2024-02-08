package net.lubble.util.converter

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import net.lubble.util.LID

class LIDStringConverter : JsonSerializer<LID>() {
    override fun serialize(p0: LID?, p1: JsonGenerator?, p2: SerializerProvider?) {
        p1?.writeString(p0?.toString())
    }

}