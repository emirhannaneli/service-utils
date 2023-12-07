package net.lubble.util.model

import jakarta.persistence.*
import java.util.*

/**
 * Base class for all JPA models.
 * It provides the following fields:
 * - id: UUID
 * - createdAt: Date
 * - updatedAt: Date
 * */
@MappedSuperclass
open class BaseJPAModel(
    @Id
    @JvmField
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @JvmField
    @Column(nullable = false, updatable = false)
    val createdAt: Date = Date(),

    @JvmField
    @Column(nullable = false)
    var updatedAt: Date = Date(),
) {
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

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}