package net.lubble.util.model

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import net.lubble.util.LID
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*

/**
 * Base class for all MongoDB models.
 * It provides the following fields:
 * - id: ObjectId
 * - deleted: Boolean
 * - createdAt: Date
 * - updatedAt: Date
 * */
@MappedSuperclass
open class BaseMongoModel(
    @Id
    var id: ObjectId? = null,

    @Indexed
    val lid: LID = LID(),

    var deleted: Boolean = false,

    @Indexed
    var createdAt: Date = Date(),

    @Indexed
    var updatedAt: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseMongoModel) return false

        if (id != other.id) return false
        if (lid != other.lid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + lid.hashCode()
        return result
    }
}