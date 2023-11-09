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
    val id: ObjectId = ObjectId.get(),

    @Indexed
    val createdAt: Date = Date(),

    @Indexed
    var updatedAt: Date = Date()
)