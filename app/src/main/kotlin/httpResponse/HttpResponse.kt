package httpResponse

import common.Constants
import common.HttpContentType
import common.HttpProtocol

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val contentType: HttpContentType? = null,
    val contentEncoding: String? = null,
    val body: String? = null
) {
    override fun toString(): String = buildString {
        // status line
        append("${protocol.value} ${status.code} ${status.message}${Constants.CRLF}")

        // headers
        contentType?.let {
            append("Content-Type: ${contentType.value}${Constants.CRLF}")
        }
        contentEncoding?.let {
            append("Content-Encoding: $it${Constants.CRLF}")
        }
        body?.let {
            append("Content-Length: ${it.toByteArray().size}${Constants.CRLF}")
        }

        // separator between Headers and body
        append(Constants.CRLF)

        // body
        if (body != null) {
            append(body)
        }
    }
}
