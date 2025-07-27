package net.lubble.util.model

import de.huxhorn.sulky.ulid.ULID
import jakarta.persistence.*
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant
import java.util.*
import kotlin.math.abs

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseModel(
    @Id
    @Column(name = "id", unique = true, updatable = false, nullable = false, length = 26)
    private var id: String = ULID().nextULID(),

    @JvmField
    @Field("pk")
    @Indexed(unique = true)
    @Column(name = "pk", unique = true, nullable = false, updatable = false, length = 12)
    var pk: Long = String.format(
        "%012d",
        abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
    ).toLong(),

    @JvmField
    @Field("sk")
    @Indexed(unique = true)
    @Convert(converter = LKToStringConverter::class)
    @Column(name = "sk", unique = true, updatable = false, nullable = false, length = 11)
    var sk: LK = LK(),

    @Indexed
    @JvmField
    @Column(nullable = false)
    var deleted: Boolean = false,

    @Indexed
    @JvmField
    @Column(nullable = false)
    var archived: Boolean = false,

    @JvmField
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @JvmField
    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    /**
     * Returns the id of the model.
     * @return The id.
     */
    open fun getId(): String {
        return id
    }

    /**
     * Sets the id of the model.
     * @param id The id to set.
     */
    open fun setId(id: String) {
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
    open fun getCreatedAt(): Instant {
        return createdAt
    }

    /**
     * Returns the date and time when the model was last updated.
     * @return The last update date and time.
     */
    open fun getUpdatedAt(): Instant {
        return updatedAt
    }

    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's pk or sk matches the given id, false otherwise.
     */
    @Suppress("unused")
    fun matchesId(id: String): Boolean {
        val value = id.toLongOrNull() ?: LK(id)
        return if (value is Long) {
            value == this.pk
        } else {
            value == this.sk
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseModel) return false

        if (id != other.id) return false
        if (pk != other.pk) return false
        if (sk != other.sk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + pk.hashCode()
        result = 31 * result + sk.hashCode()
        return result
    }
}