package net.lubble.util.model

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*

/**
 * Base class for all MongoDB models.
 * It provides the following fields:
 * - id: ObjectId
 * - createdAt: Date
 * - updatedAt: Date
 * */
@MappedSuperclass
open class BaseMongoModel(
    @Id
    var id: ObjectId? = null,

    @Indexed
    var createdAt: Date = Date(),

    @Indexed
    var updatedAt: Date = Date()


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseMongoModel) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}