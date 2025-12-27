package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import de.huxhorn.sulky.ulid.ULID
import jakarta.persistence.*
import net.lubble.util.LK
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.elasticsearch.annotations.DateFormat
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
    @field:JsonProperty(index = Int.MAX_VALUE - 3)
    open var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Basic(fetch = FetchType.EAGER)
    @Column(nullable = false, columnDefinition = "TIMESTAMP")
    @MongoField(name = "updatedAt", targetType = MongoFieldType.DATE_TIME)
    @ElasticField(name = "updatedAt", type = ElasticFieldType.Date, format = [DateFormat.epoch_millis])
    @field:JsonProperty(index = Int.MAX_VALUE - 2)
    open var updatedAt: Instant = Instant.now(),

    @CreatedBy
    @JsonIgnore
    @Basic(fetch = FetchType.EAGER)
    @MongoField(name = "created_by", targetType = MongoFieldType.STRING)
    @ElasticField(name = "created_by", type = ElasticFieldType.Keyword)
    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    @field:JsonProperty(index = Int.MAX_VALUE - 1)
    open var createdBy: String = "",

    @JsonIgnore
    @Basic(fetch = FetchType.EAGER)
    @MongoField(name = "updated_by", targetType = MongoFieldType.STRING)
    @ElasticField(name = "updated_by", type = ElasticFieldType.Keyword)
    @Column(name = "updated_by", nullable = false, updatable = true, length = 50)
    @field:JsonProperty(index = Int.MAX_VALUE)
    open var updatedBy: String = "",
    id: String, pk: Long, sk: LK,
) : LID(id, pk, sk) {

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseModel

        if (pk != other.pk) return false
        if (sk != other.sk) return false
        if (deleted != other.deleted) return false
        if (archived != other.archived) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pk.hashCode()
        result = 31 * result + sk.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + archived.hashCode()
        return result
    }

    companion object {


        fun from(source: BaseModel?): LID? {
            source ?: return null
            return LID(source)
        }
    }
}
