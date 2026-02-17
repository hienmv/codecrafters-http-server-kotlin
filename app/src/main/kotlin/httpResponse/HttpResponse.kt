package httpResponse

import common.Constant
import common.HttpContentType
import common.HttpProtocol

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val contentType: HttpContentType? = null,
    val contentEncoding: String? = null,
    val body: String? = null
) {
    override fun toString(): String {
        val statusLine = listOf(protocol.value, status.code, status.message).joinToString(" ")
        val headerLines = contentType?.let {
            val contentTypeLine = "Content-Type: ${contentType.value}"
            val contentEncodingLine = contentEncoding?.let { "Content-Encoding: $it" } ?: ""
            val contentLengthLine = body?.let { "Content-Length: ${it.toByteArray().size}" } ?: ""
            "$contentTypeLine${Constant.CRLF}$contentEncodingLine${Constant.CRLF}$contentLengthLine"
        } ?: ""
        // Status + CRLF + Headers + CRLF + CRLF + Body
        return "$statusLine${Constant.CRLF}$headerLines${Constant.CRLF}${Constant.CRLF}${body ?: ""}"
    }
}
