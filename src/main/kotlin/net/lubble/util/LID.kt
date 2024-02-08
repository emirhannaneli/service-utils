package net.lubble.util

import org.apache.commons.codec.digest.DigestUtils
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.security.SecureRandom
import java.util.*


class LID(private var seed: String) : Comparable<LID>, Serializable {
    private var value: String = ""

    private var randomInt1 = 0
    private var randomInt2 = 0
    private var randomInt3 = 0
    private var randomPartInt1 = 0
    private var randomPartInt2 = 0
    private var randomPartInt3 = 0


    constructor() : this(Generator.code().take(3), 0, 0, 0, 0, 0, 0)

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

    constructor(
        seed: String,
        randomInt1: Int,
        randomInt2: Int,
        randomInt3: Int,
        randomPartInt1: Int,
        randomPartInt2: Int,
        randomPartInt3: Int
    ) : this(seed) {
        this.randomInt1 = randomInt1
        this.randomInt2 = randomInt2
        this.randomInt3 = randomInt3
        this.randomPartInt1 = randomPartInt1
        this.randomPartInt2 = randomPartInt2
        this.randomPartInt3 = randomPartInt3

        if (this.randomInt1 == 0) this.randomInt1 = SecureRandom().nextInt(1, 10)
        if (this.randomInt2 == 0) this.randomInt2 = SecureRandom().nextInt(1, 10)
        if (this.randomInt3 == 0) this.randomInt3 = SecureRandom().nextInt(1, 10)
        val unified: StringBuilder = StringBuilder()
        val sha1Digest = DigestUtils.getSha1Digest()
        val sha256Digest = DigestUtils.getSha256Digest()
        val sha512 = DigestUtils.getSha512Digest()

        val hashSha1 = DigestUtils.digest(sha1Digest, seed.toByteArray())
        val hashSha256 = DigestUtils.digest(sha256Digest, hashSha1)
        val hashSha512 = DigestUtils.digest(sha512, hashSha256)
        val encoded = Base64.getEncoder().encodeToString(hashSha512)
        val flatBase64 = encoded
            .replace("=", this.randomInt1.toString())
            .replace("+", this.randomInt2.toString())
            .replace("/", this.randomInt3.toString())
        unified.append(flatBase64)

        val parts = unified.toString().chunked(6)
            .plus(unified.toString().chunked(this.randomInt1))
            .plus(unified.toString().chunked(this.randomInt2))
            .plus(unified.toString().chunked(this.randomInt3))
        if (randomPartInt1 == 0) this.randomPartInt1 = SecureRandom().nextInt(0, parts.size)
        if (randomPartInt2 == 0) this.randomPartInt2 = SecureRandom().nextInt(0, parts.size)
        if (randomPartInt3 == 0) this.randomPartInt3 = SecureRandom().nextInt(0, parts.size)
        val final = StringBuilder()
        final.append(parts[this.randomPartInt1].take(parts[this.randomPartInt1].length / 2))
        final.append(parts[this.randomPartInt2].take(parts[this.randomPartInt2].length / 2))
        final.append(parts[this.randomPartInt3].take(parts[this.randomPartInt3].length / 2))
        final.append(".")
        final.append(seed)
        final.append(".")
        final.append(this.randomInt1)
        final.append(".")
        final.append(this.randomInt2)
        final.append(".")
        final.append(this.randomInt3)
        final.append(".")
        final.append(this.randomPartInt1)
        final.append(".")
        final.append(this.randomPartInt2)
        final.append(".")
        final.append(this.randomPartInt3)

        value = Base64.getEncoder().encodeToString(final.toString().toByteArray())
    }

    fun toByteArray(): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { oos ->
                oos.writeObject("$seed,$randomInt1,$randomInt2,$randomInt3,$randomPartInt1,$randomPartInt2,$randomPartInt3")
            }
            return bos.toByteArray()
        }
    }

    fun toKey(): String {
        return value
    }

    override fun compareTo(other: LID): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return value
    }

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

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + seed.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    companion object {
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

        fun fromKey(key: String): LID {
            val parts = String(Base64.getDecoder().decode(key)).split(".")
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
    }
}