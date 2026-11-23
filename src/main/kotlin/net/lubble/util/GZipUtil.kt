package net.lubble.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * GZipUtil is a utility class that provides methods for compressing and decompressing data using GZIP.
 */
class GZipUtil {
    companion object {
        /**
         * Compresses the given string data into a GZIP compressed byte array.
         *
         * @param data The string data to compress.
         * @return The GZIP compressed byte array.
         */
        fun compress(data: String): ByteArray {
            return ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { it.write(data.toByteArray(StandardCharsets.UTF_8)) }
                baos.toByteArray()
            }
        }

        /**
         * Decompresses the given GZIP compressed byte array into a string.
         *
         * @param compressed The GZIP compressed byte array to decompress.
         * @return The decompressed string.
         */
        fun decompress(compressed: ByteArray): String = decompress(compressed, DEFAULT_MAX_BYTES)

        /**
         * Decompresses the given GZIP compressed byte array into a string, refusing to inflate
         * more than [maxBytes]. A few kilobytes of crafted input can otherwise expand to gigabytes.
         *
         * @param compressed The GZIP compressed byte array to decompress.
         * @param maxBytes The maximum number of decompressed bytes to accept.
         * @return The decompressed string.
         * @throws IllegalArgumentException if the decompressed data exceeds [maxBytes].
         */
        fun decompress(compressed: ByteArray, maxBytes: Int): String {
            GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
                val bytes = input.readNBytes(maxBytes)
                require(input.read() == -1) { "Decompressed data exceeds the limit of $maxBytes bytes" }
                return String(bytes, StandardCharsets.UTF_8)
            }
        }

        /** Default upper bound for [decompress]: 10 MiB. */
        const val DEFAULT_MAX_BYTES: Int = 10 * 1024 * 1024
    }
}