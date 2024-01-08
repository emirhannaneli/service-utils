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


    constructor() : this(UUID.randomUUID().toString())

    constructor(byte: ByteArray) : this(String(byte))

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

        if (randomInt1 == 0) this.randomInt1 = SecureRandom().nextInt(1, 10)
        if (randomInt2 == 0) this.randomInt2 = SecureRandom().nextInt(1, 10)
        if (randomInt3 == 0) this.randomInt3 = SecureRandom().nextInt(1, 10)
        val unified: StringBuilder = StringBuilder()
        val sha1Digest = DigestUtils.getSha1Digest()
        val sha256Digest = DigestUtils.getSha256Digest()
        val md5Digest = DigestUtils.getMd5Digest()

        val hashSha1 = DigestUtils.digest(sha1Digest, seed.toByteArray())
        println("hashSha1: ${String(hashSha1)}")
        val hashSha256 = DigestUtils.digest(sha256Digest, hashSha1)
        println("hashSha256: ${String(hashSha256)}")
        val hashMD5 = DigestUtils.digest(md5Digest, hashSha256)
        println("hashMD5: ${String(hashMD5)}")
        val encoded = Base64.getEncoder().encodeToString(hashMD5)
        println("encoded: $encoded")
        val flatBase64 = encoded
            .replace("=", randomInt1.toString())
            .replace("+", randomInt2.toString())
            .replace("/", randomInt3.toString())
        println("flatBase64: $flatBase64")
        unified.append(flatBase64)
        println("unified: $unified")

        val parts = unified.toString().chunked(4)
        println("parts: $parts")
        if (randomPartInt1 == 0) this.randomPartInt1 = SecureRandom().nextInt(0, parts.size)
        if (randomPartInt2 == 0) this.randomPartInt2 = SecureRandom().nextInt(0, parts.size)
        if (randomPartInt3 == 0) this.randomPartInt3 = SecureRandom().nextInt(0, parts.size)
        val final = StringBuilder()
        final.append(parts[randomPartInt1])
        final.append(parts[randomPartInt2])
        final.append(parts[randomPartInt3])
        println("final: $final")
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
}