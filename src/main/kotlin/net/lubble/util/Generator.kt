package net.lubble.util

import org.apache.commons.codec.digest.DigestUtils
import org.bson.types.ObjectId
import java.security.SecureRandom
import java.util.*

class Generator {
    companion object {
        private val NUMBERS = "0123456789"
        private val LOWER = "abcdefghijklmnopqrstuvwxyz"
        private val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val SPECIAL = "!@#$%^&*_=+-/.?"

        fun code(): String {
            val code = StringBuilder()
            for (i in 1..6)
                code.append(NUMBERS[(Math.random() * NUMBERS.length).toInt()])
            return code.toString()
        }

        fun password(numbers: Boolean, upper: Boolean, special: Boolean, length: Int): String {
            val password = StringBuilder()
            val chars = StringBuilder()
            chars.append(LOWER)
            if (numbers)
                chars.append(NUMBERS)
            if (upper)
                chars.append(UPPER)
            if (special)
                chars.append(SPECIAL)
            for (i in 0..<length)
                password.append(chars[(Math.random() * chars.length).toInt()])
            return password.toString()
        }
    }
}