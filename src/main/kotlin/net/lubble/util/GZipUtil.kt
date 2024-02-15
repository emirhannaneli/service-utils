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
        fun decompress(compressed: ByteArray): String {
            val reader = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(StandardCharsets.UTF_8)
            return reader.use { it.readText() }
        }
    }
}