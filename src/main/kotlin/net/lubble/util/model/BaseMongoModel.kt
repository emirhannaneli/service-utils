package net.lubble.util.model

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import net.lubble.util.LID
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*

/**
 * This is a base class for all MongoDB models. It provides common fields and methods that are used across different models.
 * @property id The ObjectId of the model. It is unique and is the primary key in MongoDB.
 * @property pk The primary key of the model. It is a Long and is unique.
 * @property sk The secondary key of the model. It is a LID and is unique.
 * @property deleted A flag indicating whether the model is deleted.
 * @property archived A flag indicating whether the model is archived.
 * @property createdAt The date and time when the model was created.
 * @property updatedAt The date and time when the model was last updated.
 * @property params The search parameters for the model. It is transient, meaning it is not persisted in the database.
 */
@MappedSuperclass
open class BaseMongoModel(
    @Id
    var id: ObjectId = ObjectId(),

    @Indexed(unique = true)
    var pk: Long = UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE,

    @Indexed(unique = true)
    var sk: LID = LID(),

    @Indexed
    var deleted: Boolean = false,

    @Indexed
    var archived: Boolean = false,

    var createdAt: Date = Date(),

    var updatedAt: Date = Date(),

    @Transient
    var params: SearchParams
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
        if (sk != other.sk) return false
        if (pk != other.pk) return false

        return true
    }

    /**
     * Returns the hash code of the model, which is based on its id, sk, and pk.
     * @return The hash code.
     */
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sk.hashCode()
        result = 31 * result + pk.hashCode()
        return result
    }

    /**
     * This class represents the search parameters for the model.
     * @property id The id to search for.
     */
    open class SearchParams : ParameterModel() {
        var id: String? = null
    }

    /**
     * Constructs a new BaseMongoModel with the given search parameters.
     * @param params The search parameters.
     */
    constructor(params: SearchParams) : this(
        id = ObjectId(),
        sk = LID(),
        pk = UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE,
        deleted = false,
        createdAt = Date(),
        updatedAt = Date(),
        params = params
    )

    /**
     * Constructs a new BaseMongoModel with default search parameters.
     */
    constructor() : this(params = SearchParams())

    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's pk or sk matches the given id, false otherwise.
     */
    fun matchesId(id: String): Boolean {
        val value = id.toLongOrNull() ?: LID.fromKey(id)
        return if (value is Long) {
            value == pk
        } else {
            value == sk
        }
    }
}