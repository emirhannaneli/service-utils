package net.lubble.util.converter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import net.lubble.util.LID

class StringToLIDConverter : JsonDeserializer<LID>() {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): LID? {
        return p?.text?.let { LID.fromKey(it) }
    }
}