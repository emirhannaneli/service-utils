package net.lubble.util.model

import jakarta.persistence.*
import net.lubble.util.LID
import java.util.*

/**
 * Base class for all JPA models.
 * It provides the following fields:
 * - pk: UUID
 * - sk: LID
 * - id: Long
 * - deleted: Boolean
 * - createdAt: Date
 * - updatedAt: Date
 * */
@MappedSuperclass
open class BaseJPAModel(
    @Id
    @JvmField
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pk", unique = true, updatable = false, nullable = false)
    val pk: UUID = UUID.randomUUID(),

    @JvmField
    @Column(name = "sk", unique = true, updatable = false, nullable = false)
    val sk: LID = LID(),

    @JvmField
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    val id: Long = pk.leastSignificantBits and Long.MAX_VALUE,

    @JvmField
    @Column(nullable = false)
    var deleted: Boolean = false,

    @JvmField
    @Column(nullable = false, updatable = false)
    val createdAt: Date = Date(),

    @JvmField
    @Column(nullable = false)
    var updatedAt: Date = Date(),

    @Transient
    open var params: SearchParams
) {

    open fun getPk(): UUID {
        return pk
    }

    open fun getSk(): LID {
        return sk
    }

    open fun getId(): Long {
        return id
    }

    open fun getDeleted(): Boolean {
        return deleted
    }

    open fun getCreatedAt(): Date {
        return createdAt
    }

    open fun getUpdatedAt(): Date {
        return updatedAt
    }

    @PrePersist
    fun prePersist() {
        updatedAt = Date()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Date()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseJPAModel) return false

        if (pk != other.pk || sk != other.sk || id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return pk.hashCode()
    }

    open class SearchParams : ParameterModel() {
        var id: String? = null
    }

    constructor(params: SearchParams) : this(UUID.randomUUID(), LID(), UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE, false, Date(), Date(), params)

    constructor() : this(SearchParams())
}