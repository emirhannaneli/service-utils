package net.lubble.util

/**
 * Generator is a utility class that provides methods for generating codes and passwords.
 */
class Generator {
    companion object {
        private val NUMBERS = "0123456789"
        private val LOWER = "abcdefghijklmnopqrstuvwxyz"
        private val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val SPECIAL = "!@#$%^&*_=+-/.?"

        /**
         * Generates a 6-digit code consisting of numbers.
         *
         * @return The generated code.
         */
        fun code(): String {
            return (1..6).map { NUMBERS[(Math.random() * NUMBERS.length).toInt()] }.joinToString("")
        }

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
            return (1..length).map { chars[(Math.random() * chars.length).toInt()] }.joinToString("")
        }
    }
}