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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, updatable = false)
    val createdAt: Date = Date(),

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
}