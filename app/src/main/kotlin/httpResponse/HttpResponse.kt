package httpResponse

import common.Constants
import common.HttpContentType
import common.HttpProtocol
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val contentType: HttpContentType? = null,
    val contentEncoding: String? = null,
    val content: String? = null
) {
    fun toBytes(): ByteArray {
        // HTTP Body is binary data
        val bodyBytes = when {
            content == null -> byteArrayOf()
            contentEncoding == "gzip" -> {
                val byteArrayOutputStream = ByteArrayOutputStream()
                GZIPOutputStream(byteArrayOutputStream).bufferedWriter(StandardCharsets.UTF_8).use {
                    it.write(content)
                }
                byteArrayOutputStream.toByteArray()
            }
            else -> content.toByteArray(StandardCharsets.UTF_8)
        }
        val headerString = buildString {
            // status line
            append("${protocol.value} ${status.code} ${status.message}${Constants.CRLF}")

            // headers
            contentType?.let {
                append("Content-Type: ${contentType.value}${Constants.CRLF}")
            }
            contentEncoding?.let {
                append("Content-Encoding: $it${Constants.CRLF}")
            }
            append("Content-Length: ${bodyBytes.size}${Constants.CRLF}")

            // separator between Headers and body
            append(Constants.CRLF)
        }
        return headerString.toByteArray(StandardCharsets.UTF_8) + bodyBytes
    }
}
