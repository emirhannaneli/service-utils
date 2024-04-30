package net.lubble.util.model

import jakarta.persistence.*
import net.lubble.util.LID
import java.util.*
import kotlin.math.abs

/**
 * This is a base class for all JPA models. It provides common fields and methods that are used across different models.
 * @property pk The primary key of the model. It is a UUID and is unique, not updatable, and not nullable.
 * @property sk The secondary key of the model. It is a LID and is unique, not updatable, and not nullable.
 * @property id The id of the model. It is a Long and is unique, not updatable, and not nullable.
 * @property deleted A flag indicating whether the model is deleted. It is not nullable.
 * @property archived A flag indicating whether the model is archived. It is not nullable.
 * @property createdAt The date and time when the model was created. It is not nullable and not updatable.
 * @property updatedAt The date and time when the model was last updated. It is not nullable.
 * @property params The search parameters for the model. It is transient, meaning it is not persisted in the database.
 */
@MappedSuperclass
open class BaseJPAModel(
    @Id
    @JvmField
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pk", unique = true, updatable = false, nullable = false)
    var pk: UUID = UUID.randomUUID(),

    @JvmField
    @Column(name = "sk", unique = true, updatable = false, nullable = false)
    var sk: LID = LID(),

    @JvmField
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    var id: Long = String.format(
        "%012d",
        abs(pk.mostSignificantBits - (pk.leastSignificantBits + System.currentTimeMillis())) % 1000000000000
    ).toLong(),

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
     * Returns the primary key of the model.
     * @return The primary key.
     */
    open fun getPk(): UUID {
        return pk
    }

    /**
     * Returns the secondary key of the model.
     * @return The secondary key.
     */
    open fun getSk(): LID {
        return sk
    }

    /**
     * Returns the id of the model.
     * @return The id.
     */
    open fun getId(): Long {
        return id
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

        if (pk != other.pk || sk != other.sk || id != other.id) return false
        return true
    }

    /**
     * Returns the hash code of the model, which is based on its pk.
     * @return The hash code.
     */
    override fun hashCode(): Int {
        return pk.hashCode()
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
        val value = id.toLongOrNull() ?: LID.fromKey(id)
        return if (value is Long) {
            this.id == value
        } else {
            this.sk == value
        }
    }
}