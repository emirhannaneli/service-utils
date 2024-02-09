package net.lubble.util.model

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import net.lubble.util.LID
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*

/**
 * Base class for all MongoDB models.
 * It provides the following fields:
 * - id: ObjectId
 * - sk: LID
 * - pk: Long
 * - deleted: Boolean
 * - createdAt: Date
 * - updatedAt: Date
 * */
@MappedSuperclass
open class BaseMongoModel(
    @Id
    var id: ObjectId = ObjectId(),

    @Indexed(unique = true)
    var pk: Long = UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE,

    @Indexed(unique = true)
    val sk: LID = LID(),

    @Indexed
    var deleted: Boolean = false,

    var createdAt: Date = Date(),

    var updatedAt: Date = Date(),

    @Transient
    var params: SearchParams
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseMongoModel) return false

        if (id != other.id) return false
        if (sk != other.sk) return false
        if (pk != other.pk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sk.hashCode()
        result = 31 * result + pk.hashCode()
        return result
    }

    open class SearchParams : ParameterModel() {
        var id: String? = null
    }

    constructor(params: SearchParams) : this(
        id = ObjectId(),
        sk = LID(),
        pk = UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE,
        deleted = false,
        createdAt = Date(),
        updatedAt = Date(),
        params = params
    )

    constructor() : this(params = SearchParams())

    fun matchesId(id: String): Boolean {
        val value = id.toLongOrNull() ?: LID.fromKey(id)
        return if (value is Long) {
            value == pk
        } else {
            value == sk
        }
    }
}