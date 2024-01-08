package net.lubble.util

import org.apache.commons.codec.digest.DigestUtils
import java.io.Serializable
import java.security.SecureRandom
import java.util.*

class LID(private val seed: String) : Comparable<LID>, Serializable {
    private var value: String = ""

    private var randomInt1 = 0
    private var randomInt2 = 0
    private var randomInt3 = 0
    private var randomPartInt1 = 0
    private var randomPartInt2 = 0
    private var randomPartInt3 = 0


    constructor() : this(UUID.randomUUID().toString(), 0, 0, 0, 0, 0, 0)

    constructor(byte: ByteArray) : this(String(byte), 0, 0, 0, 0, 0, 0)

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
        final.append(parts[this.randomPartInt1])
        final.append(parts[this.randomPartInt2])
        final.append(parts[this.randomPartInt3])
        value = final.toString()
    }

    fun toByteArray(): ByteArray {
        return value.toByteArray()
    }

    fun toKey(): String {
        return value
    }

    override fun compareTo(other: LID): Int {
        return value.compareTo(other.value)
    }

    override fun toString(): String {
        return "LID(\nvalue='$value', \nseed='$seed', \nrandomInt1=$randomInt1, \nrandomInt2=$randomInt2, \nrandomInt3=$randomInt3, \nrandomPartInt1=$randomPartInt1, \nrandomPartInt2=$randomPartInt2, \nrandomPartInt3=$randomPartInt3)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is LID) {
            return value == other.value
        }
        return false
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + seed.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    companion object{
        @JvmStatic
        fun main(args: Array<String>) {
            val lid = LID()
            println("lid: $lid")
        }
    }
}