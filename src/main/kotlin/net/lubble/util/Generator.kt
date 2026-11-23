package net.lubble.util

import java.security.SecureRandom

/**
 * Generator is a utility class that provides methods for generating codes and passwords.
 */
class Generator {
    companion object {
        private const val NUMBERS = "0123456789"
        private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
        private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val SPECIAL = "!@#$%^&*_=+-/.?"

        // Codes and passwords are secrets: they must come from a CSPRNG, never Math.random().
        private val RANDOM = SecureRandom()

        private fun random(chars: CharSequence, length: Int): String =
            String(CharArray(length) { chars[RANDOM.nextInt(chars.length)] })

        /**
         * Generates a 6-digit code consisting of numbers.
         *
         * @return The generated code.
         */
        fun code(): String = random(NUMBERS, 6)

        /**
         * Generates a password with the specified characteristics.
         *
         * @param numbers Whether the password should include numbers.
         * @param upper Whether the password should include uppercase letters.
         * @param special Whether the password should include special characters.
         * @param length The length of the password.
         * @return The generated password.
         */
        fun password(numbers: Boolean, upper: Boolean, special: Boolean, length: Int): String {
            val chars = StringBuilder(LOWER)
            if (numbers) chars.append(NUMBERS)
            if (upper) chars.append(UPPER)
            if (special) chars.append(SPECIAL)
            return random(chars, length)
        }
    }
}
