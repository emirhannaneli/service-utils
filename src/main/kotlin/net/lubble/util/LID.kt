package net.lubble.util

import org.apache.commons.codec.digest.DigestUtils
import java.io.Serializable
import java.security.SecureRandom
import java.util.*

class LID(private var value: String) : Comparable<LID>, Serializable {
    private val uuid: UUID = UUID.randomUUID()

    init {
        if (value.isEmpty()) {
            val unified: StringBuilder = StringBuilder()
            val sha1Digest = DigestUtils.getSha1Digest()
            val sha256Digest = DigestUtils.getSha256Digest()
            val md5Digest = DigestUtils.getMd5Digest()

            for (i in 1..6) {
                val random = SecureRandom()
                val hashCode = uuid.hashCode()
                val hashSha1 = DigestUtils.digest(sha1Digest, hashCode.toString().toByteArray())
                val hashSha256 = DigestUtils.digest(sha256Digest, hashSha1)
                val hashMD5 = DigestUtils.digest(md5Digest, hashSha256)
                val encoded = Base64.getEncoder().encodeToString(hashMD5)
                val flatBase64 = encoded
                    .replace("=", random.nextInt(1, 10).toString())
                    .replace("+", random.nextInt(1, 10).toString())
                    .replace("/", random.nextInt(1, 10).toString())
                unified.append(flatBase64)
            }
            val parts = unified.toString().chunked(5)
            val final = StringBuilder()
            for (i in 1..3) {
                val part = parts[(Math.random() * parts.size).toInt()]
                final.append(part)
            }
            value = final.toString()
        }
    }

    constructor() : this("")

    constructor(byte: ByteArray?) : this(String(byte ?: ByteArray(0)))

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
        return "LID(value='$value', uuid=$uuid)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is LID) {
            return value == other.value
        }
        return false
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }


}