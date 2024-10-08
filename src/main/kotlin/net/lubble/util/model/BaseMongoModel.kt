package net.lubble.util.model

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import net.lubble.util.LK
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Field
import java.util.*
import kotlin.math.abs

/**
 * This is a base class for all MongoDB models. It provides common fields and methods that are used across different models.
 * @property id The ObjectId of the model. It is unique and is the primary key in MongoDB.
 * @property pk The primary key of the model. It is a Long and is unique.
 * @property sk The secondary key of the model. It is a LK and is unique.
 * @property deleted A flag indicating whether the model is deleted.
 * @property archived A flag indicating whether the model is archived.
 * @property createdAt The date and time when the model was created.
 * @property updatedAt The date and time when the model was last updated.
 */
@MappedSuperclass
open class BaseMongoModel(
    @Id
    private var id: ObjectId = ObjectId(),

    @Field("pk")
    @Indexed(unique = true)
    var pk: Long = String.format(
        "%012d",
        abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
    ).toLong(),

    @Field("sk")
    @Indexed(unique = true)
    var sk: LK = LK(),

    @Indexed
    var deleted: Boolean = false,

    @Indexed
    var archived: Boolean = false,

    var createdAt: Date = Date(),

    var updatedAt: Date = Date(),

    ) {

    /**
     * Checks if the model is equal to another object.
     * @param other The object to compare with.
     * @return True if the other object is a BaseMongoModel and has the same id, sk, and pk, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseMongoModel) return false

        if (id != other.id) return false
        if (pk != other.pk) return false
        if (sk != other.sk) return false

        return true
    }

    /**
     * Returns the hash code of the model, which is based on its id, sk, and pk.
     * @return The hash code.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + pk.hashCode()
        result = 31 * result + sk.hashCode()
        return result
    }

    /**
     * This class represents the search parameters for the model.
     * @property pk The id to search for.
     */
    @Suppress("unused")
    open class SearchParams : ParameterModel()

    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's pk or sk matches the given id, false otherwise.
     */
    @Suppress("unused")
    fun matchesId(id: String): Boolean {
        val value = id.toLongOrNull() ?: LK(id)
        return if (value is Long) {
            value == this.pk
        } else {
            value == this.sk
        }
    }
}