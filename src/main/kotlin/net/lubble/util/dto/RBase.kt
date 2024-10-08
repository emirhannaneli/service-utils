package net.lubble.util.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import java.util.*

/**
 * This is a base class for DTOs (Data Transfer Objects).
 * It includes common fields that are present in most DTOs.
 *
 * The fields include:
 * - id: A unique identifier for the DTO.
 * - sk: A secondary key for the DTO.
 * - updatedAt: The date and time when the DTO was last updated.
 * - createdAt: The date and time when the DTO was created.
 */
open class RBase {
    /**
     * The unique identifier for the DTO.
     */
    @JsonProperty(index = 0)
    open var pk: Any? = null

    /**
     * A secondary key for the DTO.
     */
    @JsonProperty(index = 1)
    @JsonSerialize(using = LKToStringConverter.Serializer::class)
    @JsonDeserialize(using = LKToStringConverter.Deserializer::class)
    open var sk: LK? = null

    /**
     * The date and time when the DTO was last updated.
     */
    @JsonProperty(index = Int.MAX_VALUE - 1)
    open var updatedAt: Date? = null

    /**
     * The date and time when the DTO was created.
     */
    @JsonProperty(index = Int.MAX_VALUE)
    open var createdAt: Date? = null
}