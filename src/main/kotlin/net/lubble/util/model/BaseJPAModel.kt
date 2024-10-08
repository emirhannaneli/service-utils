package net.lubble.util.model

import jakarta.persistence.*
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import java.util.*
import kotlin.math.abs

/**
 * This is a base class for all JPA models. It provides common fields and methods that are used across different models.
 * @property id The primary key of the model. It is a UUID and is unique, not updatable, and not nullable.
 * @property sk The secondary key of the model. It is a LK and is unique, not updatable, and not nullable.
 * @property pk The id of the model. It is a Long and is unique, not updatable, and not nullable.
 * @property deleted A flag indicating whether the model is deleted. It is not nullable.
 * @property archived A flag indicating whether the model is archived. It is not nullable.
 * @property createdAt The date and time when the model was created. It is not nullable and not updatable.
 * @property updatedAt The date and time when the model was last updated. It is not nullable.
 */
@MappedSuperclass
open class BaseJPAModel(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, updatable = false, nullable = false, length = 36)
    private var id: UUID = UUID.randomUUID(),

    @JvmField
    @Column(name = "pk", unique = true, nullable = false, updatable = false, length = 12)
    var pk: Long = String.format(
        "%012d",
        abs(id.mostSignificantBits - (id.leastSignificantBits + System.currentTimeMillis())) % 1000000000000
    ).toLong(),

    @JvmField
    @Convert(converter = LKToStringConverter::class)
    @Column(name = "sk", unique = true, updatable = false, nullable = false, length = 11)
    var sk: LK = LK(),

    @JvmField
    @Column(nullable = false)
    var deleted: Boolean = false,

    @JvmField
    @Column(nullable = false)
    var archived: Boolean = false,

    @JvmField
    @Column(nullable = false, updatable = false)
    var createdAt: Date = Date(),

    @JvmField
    @Column(nullable = false)
    var updatedAt: Date = Date(),
) {

    /**
     * Returns the id of the model.
     * @return The id.
     */
    open fun getId(): UUID {
        return id
    }

    /**
     * Sets the id of the model.
     * @param id The id to set.
     */
    open fun setId(id: UUID) {
        this.id = id
    }

    /**
     * Returns the primary key of the model.
     * @return The primary key.
     */
    open fun getPk(): Long {
        return pk
    }

    /**
     * Returns the secondary key of the model.
     * @return The secondary key.
     */
    open fun getSk(): LK {
        return sk
    }

    /**
     * Returns whether the model is deleted.
     * @return True if the model is deleted, false otherwise.
     */
    open fun getDeleted(): Boolean {
        return deleted
    }

    /**
     * Returns whether the model is archived.
     * @return True if the model is archived, false otherwise.
     */
    open fun getArchived(): Boolean {
        return archived
    }

    /**
     * Returns the date and time when the model was created.
     * @return The creation date and time.
     */
    open fun getCreatedAt(): Date {
        return createdAt
    }

    /**
     * Returns the date and time when the model was last updated.
     * @return The last update date and time.
     */
    open fun getUpdatedAt(): Date {
        return updatedAt
    }

    /**
     * Updates the updatedAt field before the model is persisted.
     */
    @PrePersist
    fun prePersist() {
        updatedAt = Date()
    }

    /**
     * Updates the updatedAt field before the model is updated.
     */
    @PreUpdate
    fun preUpdate() {
        updatedAt = Date()
    }

    /**
     * Checks if the model is equal to another object.
     * @param other The object to compare with.
     * @return True if the other object is a BaseJPAModel and has the same pk, sk, and id, false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseJPAModel) return false

        if (id != other.id || sk != other.sk || pk != other.pk) return false
        return true
    }

    /**
     * Returns the hash code of the model, which is based on its pk.
     * @return The hash code.
     */
    override fun hashCode(): Int {
        return id.hashCode()
    }

    /**
     * This class represents the search parameters for the model.
     * @property deleted A flag indicating whether the model is deleted.
     * @property archived A flag indicating whether the model is archived.
     */
    open class SearchParams : ParameterModel()

    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's id or sk matches the given id, false otherwise.
     */
    fun matchesId(id: String): Boolean {
        val value = id.toLongOrNull() ?: LK(id)
        return if (value is Long) {
            this.pk == value
        } else {
            this.sk == value
        }
    }
}