package net.lubble.util

import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.security.SecureRandom
import java.util.*

/**
 * LID class is used to generate a unique identifier based on a seed string.
 * The identifier is a combination of hashed seed, random integers and parts of the hashed seed.
 * The class implements Comparable and Serializable interfaces.
 *
 * @property seed The seed string used to generate the unique identifier.
 */
class LID(private var seed: String = Generator.password(numbers = true, upper = true, special = false, length = 5)) : Comparable<LID>, Serializable {
    private var value: String = ""

    private var randomInt1 = 0
    private var randomInt2 = 0
    private var randomInt3 = 0
    private var randomPartInt1 = 0
    private var randomPartInt2 = 0
    private var randomPartInt3 = 0

    /**
     * Default constructor that generates random integers and parts.
     */
    constructor() : this(Generator.code().take(3), 0, 0, 0, 0, 0, 0)

    /**
     * Constructor that initializes the LID object from a byte array.
     *
     * @param byte The byte array to initialize the LID object from.
     */
    constructor(byte: ByteArray) : this() {
        val obj = fromByteArray(byte)
        this.seed = obj.seed
        this.randomInt1 = obj.randomInt1
        this.randomInt2 = obj.randomInt2
        this.randomInt3 = obj.randomInt3
        this.randomPartInt1 = obj.randomPartInt1
        this.randomPartInt2 = obj.randomPartInt2
        this.randomPartInt3 = obj.randomPartInt3
    }

    /**
     * Constructor that initializes the LID object with the provided seed and random integers and parts.
     * If any of the random integers or parts is zero, a new random integer or part is generated.
     *
     * @param seed The seed string.
     * @param randomInt1 The first random integer.
     * @param randomInt2 The second random integer.
     * @param randomInt3 The third random integer.
     * @param randomPartInt1 The first random part.
     * @param randomPartInt2 The second random part.
     * @param randomPartInt3 The third random part.
     */
    constructor(
        seed: String,
        randomInt1: Int,
        randomInt2: Int,
        randomInt3: Int,
        randomPartInt1: Int,
        randomPartInt2: Int,
        randomPartInt3: Int
    ) : this(seed) {
        this.randomInt1 = randomInt1.takeIf { it != 0 } ?: SecureRandom().nextInt(1, 10)
        this.randomInt2 = randomInt2.takeIf { it != 0 } ?: SecureRandom().nextInt(1, 10)
        this.randomInt3 = randomInt3.takeIf { it != 0 } ?: SecureRandom().nextInt(1, 10)

        val unified = StringBuilder()
        val hash = DigestUtils.sha512(seed.toByteArray())
        val encoded = Base64.getEncoder().encodeToString(hash)
        val flatBase64 = encoded
            .replace("=", this.randomInt1.toString())
            .replace("+", this.randomInt2.toString())
            .replace("/", this.randomInt3.toString())
        unified.append(flatBase64)

        val parts = unified.toString().chunked(6)
            .plus(unified.toString().chunked(this.randomInt1))
            .plus(unified.toString().chunked(this.randomInt2))
            .plus(unified.toString().chunked(this.randomInt3))

        this.randomPartInt1 = randomPartInt1.takeIf { it != 0 } ?: SecureRandom().nextInt(0, parts.size)
        this.randomPartInt2 = randomPartInt2.takeIf { it != 0 } ?: SecureRandom().nextInt(0, parts.size)
        this.randomPartInt3 = randomPartInt3.takeIf { it != 0 } ?: SecureRandom().nextInt(0, parts.size)

        val final = StringBuilder()
        final.append(parts[this.randomPartInt1].take(parts[this.randomPartInt1].length / 2))
        final.append(parts[this.randomPartInt2].take(parts[this.randomPartInt2].length / 2))
        final.append(parts[this.randomPartInt3].take(parts[this.randomPartInt3].length / 2))
        final.append(".$seed.${this.randomInt1}.${this.randomInt2}.${this.randomInt3}.${this.randomPartInt1}.${this.randomPartInt2}.${this.randomPartInt3}")

        value = Base64.getUrlEncoder().encodeToString(final.toString().toByteArray()).trimEnd('=')
    }

    /**
     * Converts the LID object to a byte array.
     *
     * @return The byte array representation of the LID object.
     */
    fun toByteArray(): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject("$seed,$randomInt1,$randomInt2,$randomInt3,$randomPartInt1,$randomPartInt2,$randomPartInt3")
            }
            return bos.toByteArray()
        }
    }

    /**
     * Converts the LID object to a key string.
     *
     * @return The key string representation of the LID object.
     */
    fun toKey(): String {
        return value
    }

    /**
     * Compares this LID object with the specified LID object for order.
     *
     * @param other The LID object to be compared.
     * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    override fun compareTo(other: LID): Int {
        return value.compareTo(other.value)
    }

    /**
     * Returns a string representation of the LID object.
     *
     * @return A string representation of the LID object.
     */
    override fun toString(): String {
        return value
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (other is LID) {
            return value == other.value
                    && seed == other.seed
                    && randomInt1 == other.randomInt1
                    && randomInt2 == other.randomInt2
                    && randomInt3 == other.randomInt3
                    && randomPartInt1 == other.randomPartInt1
                    && randomPartInt2 == other.randomPartInt2
                    && randomPartInt3 == other.randomPartInt3
        }
        return false
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + seed.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    companion object {
        /**
         * Creates a LID object from a byte array.
         *
         * @param byte The byte array to create the LID object from.
         * @return The created LID object.
         */
        fun fromByteArray(byte: ByteArray): LID {
            ObjectInputStream(byte.inputStream()).use { ois ->
                val obj = ois.readObject()
                if (obj is String) {
                    val parts = obj.split(",")
                    return LID(
                        parts[0],
                        parts[1].toInt(),
                        parts[2].toInt(),
                        parts[3].toInt(),
                        parts[4].toInt(),
                        parts[5].toInt(),
                        parts[6].toInt()
                    )
                }
            }
            return LID()
        }

        /**
         * Creates a LID object from a key string.
         *
         * @param key The key string to create the LID object from.
         * @return The created LID object.
         */
        fun fromKey(key: String): LID {
            val parts = String(Base64.getUrlDecoder().decode(key)).split(".")
            return LID(
                parts[1],
                parts[2].toInt(),
                parts[3].toInt(),
                parts[4].toInt(),
                parts[5].toInt(),
                parts[6].toInt(),
                parts[7].toInt()
            )
        }

        fun fromKeyOrNull(key: String): LID? {
            return try {
                fromKey(key)
            } catch (e: Exception) {
                null
            }
        }
    }
}