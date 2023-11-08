package net.lubble.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GZipUtil {
    companion object {
        fun compress(data: String): ByteArray {
            val bytes = data.toByteArray(StandardCharsets.UTF_8)
            val baos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(baos)
            gzip.write(bytes)
            gzip.close()
            return baos.toByteArray()
        }

        fun decompress(compressed: ByteArray): String {
            val bais = ByteArrayInputStream(compressed)
            val gzip = GZIPInputStream(bais)
            val reader = InputStreamReader(gzip, StandardCharsets.UTF_8)
            val writer = StringWriter()
            val buffer = CharArray(10240)
            var length: Int
            while (reader.read(buffer).also { length = it } > 0) {
                writer.write(buffer, 0, length)
            }
            return writer.toString()
        }
    }
}