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
        val random = RANDOM
        key.clear()
        repeat(3) { i ->
            repeat(5) {
                key.append(CHARS[random.nextInt(CHARS.length)])
            }
            if (i < 2) key.append("-")
        }
        value = key.toString()
    }

    constructor(key: String) {
        this.key.clear()
        this.key.append(key)
        this.value = key
    }

    companion object {
        private const val CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

        // BC's "DEFAULT" SecureRandom is expensive to build (provider lookup +
        // service instantiation + entropy seeding); cache one instance per JVM
        // instead of allocating a new one in every LK() constructor.
        private val RANDOM: SecureRandom by lazy {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
            SecureRandom.getInstance("DEFAULT", "BC")
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