package infrastructure.http

import domain.httpResponse.HttpResponse
import domain.vo.HttpContentEncoding
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

object HttpResponseSerializer {
    fun serialize(response: HttpResponse): ByteArray {
        // HTTP Body is binary data
        val bodyBytes =
            when {
                response.body == null -> byteArrayOf()
                response.headers["Content-Encoding"] == HttpContentEncoding.GZIP.value -> {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    GZIPOutputStream(byteArrayOutputStream).use {
                        it.write(response.body)
                    }
                    byteArrayOutputStream.toByteArray()
                }
                else -> response.body
            }
        val headerString =
            buildString {
                // status line
                append("${response.protocol.value} ${response.status.code} ${response.status.message}\r\n")

                // headers
                response.headers.forEach { (key, value) -> append("$key: $value\r\n") }
                append("Content-Length: ${bodyBytes.size}\r\n")

                // separator CRLF between Headers and body
                append("\r\n")
            }
        return headerString.toByteArray(StandardCharsets.UTF_8) + bodyBytes
    }
}
