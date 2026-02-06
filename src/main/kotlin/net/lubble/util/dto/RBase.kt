package net.lubble.util.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import java.time.Instant


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
    @JsonProperty(index = Int.MIN_VALUE)
    open var pk: Long? = null

    @JsonProperty(index = Int.MIN_VALUE + 1)
    @JsonSerialize(using = LKToStringConverter.Serializer.Json::class)
    @JsonDeserialize(using = LKToStringConverter.Deserializer.Json::class)
    open var sk: LK? = null

    @JsonProperty(index = Int.MAX_VALUE - 3)
    open var updatedAt: Instant? = null
    @JsonProperty(index = Int.MAX_VALUE - 2)
    open var createdAt: Instant? = null
    @JsonProperty(index = Int.MAX_VALUE - 1)
    open var createdBy: Any? = null
    @JsonProperty(index = Int.MAX_VALUE - 1)
    open var updatedBy: Any? = null

    @JsonIgnore
    open var archived: Boolean? = null

    @JsonIgnore
    open var deleted: Boolean? = null
}