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
    open var pk: Long? = null

    @JsonSerialize(using = LKToStringConverter.Serializer.Json::class)
    @JsonDeserialize(using = LKToStringConverter.Deserializer.Json::class)
    open var sk: LK? = null

    open var updatedAt: Instant? = null

    open var createdAt: Instant? = null

    open var createdBy: Any? = null

    open var updatedBy: Any? = null

    @JsonIgnore
    open var archived: Boolean? = null

    @JsonIgnore
    open var deleted: Boolean? = null
}