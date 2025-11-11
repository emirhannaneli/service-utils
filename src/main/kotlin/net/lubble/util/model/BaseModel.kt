package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.huxhorn.sulky.ulid.ULID
import jakarta.persistence.*
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.ValueConverter
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.mongodb.core.index.Indexed
import java.time.Instant
import java.util.*
import kotlin.math.abs
import org.springframework.data.elasticsearch.annotations.Field as ElasticField
import org.springframework.data.elasticsearch.annotations.FieldType as ElasticFieldType
import org.springframework.data.mongodb.core.mapping.Field as MongoField
import org.springframework.data.mongodb.core.mapping.FieldType as MongoFieldType

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
open class BaseModel(
    @Id
    @field:JsonIgnore
    @Column(name = "id", unique = true, updatable = false, nullable = false, length = 26)
    private var id: String,

    @Indexed(unique = true)
    @MongoField("pk")
    @Basic(fetch = FetchType.EAGER)
    @ElasticField("pk", type = ElasticFieldType.Keyword, index = true)
    @field:JsonProperty(index = Int.MIN_VALUE)
    @Column(name = "pk", unique = true, nullable = false, updatable = false, length = 12)
    open var pk: Long,

    @Indexed(unique = true)
    @Basic(fetch = FetchType.EAGER)
    @MongoField("sk", targetType = MongoFieldType.STRING)
    @ElasticField("sk", type = ElasticFieldType.Keyword, index = true)
    @ValueConverter(LKToStringConverter::class)
    @Convert(converter = LKToStringConverter::class)
    @Column(
        name = "sk",
        unique = true,
        updatable = false,
        nullable = false,
        length = 11,
        columnDefinition = "varchar(11)"
    )
    @field:JsonProperty(index = Int.MIN_VALUE + 1)
    @field:JsonSerialize(using = LKToStringConverter.Serializer::class)
    @field:JsonDeserialize(using = LKToStringConverter.Deserializer::class)
    open var sk: LK,

    @Indexed
    @JsonIgnore
    @Basic(fetch = FetchType.EAGER)
    @ElasticField(name = "deleted", type = ElasticFieldType.Boolean, index = true)
    @Column(nullable = false)
    open var deleted: Boolean = false,

    @Indexed
    @JsonIgnore
    @Basic(fetch = FetchType.EAGER)
    @ElasticField(name = "archived", type = ElasticFieldType.Boolean, index = true)
    @Column(nullable = false)
    open var archived: Boolean = false,

    @CreatedDate
    @Basic(fetch = FetchType.EAGER)
    @MongoField(name = "createdAt", targetType = MongoFieldType.DATE_TIME)
    @ElasticField(name = "createdAt", type = ElasticFieldType.Date, format = [DateFormat.epoch_millis])
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @field:JsonProperty(index = Int.MAX_VALUE - 1)
    open var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Basic(fetch = FetchType.EAGER)
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    @MongoField(name = "updatedAt", targetType = MongoFieldType.DATE_TIME)
    @ElasticField(name = "updatedAt", type = ElasticFieldType.Date, format = [DateFormat.epoch_millis])
    @field:JsonProperty(index = Int.MAX_VALUE)
    open var updatedAt: Instant = Instant.now(),
) {

    /**
     * Generates a new BaseModel with a unique id and the given pk and sk.
     * @param pk The partition key.
     * @param sk The sort key.
     * */
    constructor(pk: Long, sk: LK) : this(
        id = ULID().nextULID(),
        pk = pk,
        sk = sk
    )

    /**
     * Generates a new BaseModel with a unique id, pk, and sk.
     * */
    constructor() : this(
        id = ULID().nextULID(),
        pk = String.format(
            "%012d",
            abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
        ).toLong(),
        sk = LK()
    )

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
     * Returns the pk of the model.
     * @return The pk.
     */
    /*@Basic(fetch = FetchType.EAGER)
    open fun getPk(): Long {
        return pk
    }*/

    /*open fun setPk(pk: Long) {
        this.pk = pk
    }*/

    /**
     * Returns the sk of the model.
     * @return The sk.
     */
    /*@Basic(fetch = FetchType.EAGER)
    open fun getSk(): LK {
        return sk
    }*/

    /*open fun setSk(sk: LK) {
        this.sk = sk
    }*/

    /**
     * Returns whether the model is deleted.
     * @return True if the model is deleted, false otherwise.
     */
    /*@Basic(fetch = FetchType.EAGER)
    open fun getDeleted(): Boolean {
        return deleted
    }*/

    /*open fun setDeleted(deleted: Boolean) {
        this.deleted = deleted
    }*/

    /**
     * Returns whether the model is archived.
     * @return True if the model is archived, false otherwise.
     */
    @Basic(fetch = FetchType.EAGER)
    /*open fun getArchived(): Boolean {
        return archived
    }*/

    /*open fun setArchived(archived: Boolean) {
        this.archived = archived
    }*/


    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's pk or sk matches the given id, false otherwise.
     */
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