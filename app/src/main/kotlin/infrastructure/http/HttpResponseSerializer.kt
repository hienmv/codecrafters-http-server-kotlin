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

                // headers — strip \r and \n before writing to the wire.
                // A value containing \r\n would be interpreted as a new header line by the client
                // (CRLF injection / response splitting). RFC 7230 forbids \r\n inside a header
                // value; any occurrence is either a bug or an attack.
                response.headers.forEach { (key, value) ->
                    val safeKey = key.replace("\r", "").replace("\n", "")
                    val safeValue = value.replace("\r", "").replace("\n", "")
                    append("$safeKey: $safeValue\r\n")
                }
                append("Content-Length: ${bodyBytes.size}\r\n")

                // separator CRLF between Headers and body
                append("\r\n")
            }
        return headerString.toByteArray(StandardCharsets.UTF_8) + bodyBytes
    }
}
