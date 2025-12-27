package net.lubble.util.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.huxhorn.sulky.ulid.ULID
import jakarta.persistence.*
import net.lubble.util.LK
import net.lubble.util.converter.LKToStringConverter
import org.springframework.data.elasticsearch.annotations.ValueConverter
import org.springframework.data.mongodb.core.index.Indexed
import java.util.*
import kotlin.math.abs
import org.springframework.data.elasticsearch.annotations.Field as ElasticField
import org.springframework.data.elasticsearch.annotations.FieldType as ElasticFieldType
import org.springframework.data.mongodb.core.mapping.Field as MongoField
import org.springframework.data.mongodb.core.mapping.FieldType as MongoFieldType

@MappedSuperclass
open class LID(
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
) {

    /**
     * Default constructor generating unique id, pk, and sk.
     */
    constructor() : this(
        id = ULID().nextULID(),
        pk = String.format(
            "%012d",
            abs(UUID.randomUUID().mostSignificantBits - (UUID.randomUUID().leastSignificantBits + System.currentTimeMillis())) % 1000000000000
        ).toLong(),
        sk = LK()
    )

    /**
     * Constructor generating unique id and sk.
     * @param pk The partition key.
     */
    constructor(pk: Long, sk: LK) : this(
        id = ULID().nextULID(),
        pk = pk,
        sk = sk
    )

    /**
     * Copy constructor from BaseModel.
     * @param source The source BaseModel.
     */
    constructor(source: BaseModel) : this(
        id = source.getId(),
        pk = source.pk,
        sk = source.sk
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
     * Checks if this LID matches another LID.
     * @param lid The LID to compare with.
     * @return True if both pk and sk are equal, false otherwise.
     */
    fun matches(lid: LID?): Boolean {
        if (lid == null) return false
        return this.pk == lid.pk && this.sk == lid.sk
    }

    /**
     * Checks if this LID matches the given pk.
     * @param pk The pk to compare with.
     * @return True if pk matches, false otherwise.
     */
    fun matches(pk: Long): Boolean {
        return this.pk == pk
    }

    /**
     * Checks if this LID matches the given sk.
     * @param sk The sk to compare with.
     * @return True if sk matches, false otherwise.
     */
    fun matches(sk: LK): Boolean {
        return this.sk == sk
    }

    /**
     * Checks if the model matches the given id.
     * @param id The id to check.
     * @return True if the model's pk or sk matches the given id, false otherwise.
     */
    fun matches(id: String): Boolean {
        val value = id.toLongOrNull() ?: LK(id)
        return if (value is Long) {
            value == this.pk
        } else {
            value == this.sk
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LID

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