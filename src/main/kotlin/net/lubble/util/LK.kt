package net.lubble.util

import com.fasterxml.jackson.annotation.JsonValue
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.data.annotation.Transient
import java.io.Serializable
import java.security.SecureRandom
import java.security.Security

class LK : Comparable<LK>, Serializable {
    var value: String

    @Transient
    private var key: StringBuilder = StringBuilder()

    constructor() {
        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom.getInstance("DEFAULT", "BC")

        key.clear()
        repeat(2) { i ->
            repeat(5) {
                key.append(characters[random.nextInt(characters.length)])
            }
            if (i < 1) key.append("-")
        }
        value = key.toString()
    }

    constructor(key: String) {
        this.key.clear()
        this.key.append(key)
        this.value = key
    }

    companion object {
        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    @JsonValue
    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as LK
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun compareTo(other: LK): Int {
        return value.compareTo(other.value)
    }
}