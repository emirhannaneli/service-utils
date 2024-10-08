package net.lubble.util

import com.fasterxml.jackson.annotation.JsonValue
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.data.annotation.Transient
import java.io.Serializable
import java.security.SecureRandom
import java.security.Security

class LK() : Comparable<LK>, Serializable {
    var value: String

    @Transient
    private var key: StringBuilder = StringBuilder()

    constructor(key: String) : this() {
        this.key.clear()
        this.key.append(key)

        value = key
    }

    init {
        Security.addProvider(BouncyCastleProvider())

        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom.getInstance("DEFAULT", "BC")

        repeat(2) {
            repeat(5) {
                key.append(characters[random.nextInt(characters.length)])
            }
            if (it < 1) key.append("-")
        }

        value = key.toString()
    }

    @JsonValue
    override fun toString(): String {
        return key.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        other as LK

        return key.toString() == other.key.toString()
    }

    override fun hashCode(): Int {
        return key.toString().hashCode()
    }

    override fun compareTo(other: LK): Int {
        return key.toString().compareTo(other.key.toString())
    }
}