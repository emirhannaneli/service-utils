package net.lubble.util.model

import jakarta.persistence.*
import net.lubble.util.LID
import java.util.*

/**
 * Base class for all JPA models.
 * It provides the following fields:
 * - pk: UUID
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
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    val id: LID = LID(),

    @JvmField
    @Column(nullable = false)
    var deleted: Boolean = false,

    @JvmField
    @Column(nullable = false, updatable = false)
    val createdAt: Date = Date(),

    @JvmField
    @Column(nullable = false)
    var updatedAt: Date = Date(),
) {

    fun getPk(): UUID {
        return pk
    }

    fun getId(): LID {
        return id
    }

    fun getDeleted(): Boolean {
        return deleted
    }

    fun getCreatedAt(): Date {
        return createdAt
    }

    fun getUpdatedAt(): Date {
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

        if (pk != other.pk) return false

        return true
    }

    override fun hashCode(): Int {
        return pk.hashCode()
    }
}