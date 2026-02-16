package httpResponse

import common.Constant
import common.HttpContentType
import common.HttpProtocol

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val contentType: HttpContentType? = null,
    val content: String? = null
) {
    override fun toString(): String {
        val statusLine = listOf(protocol.value, status.code, status.message).joinToString(" ")
        val body = content ?: ""
        val headerLines = contentType?.let {
            "Content-Type: ${contentType.value}${Constant.CRLF}Content-Length: ${body.toByteArray().size}"
        } ?: ""
        // Status + CRLF + Headers + CRLF + CRLF + Body
        return "$statusLine${Constant.CRLF}$headerLines${Constant.CRLF}${Constant.CRLF}$body"
    }
}
