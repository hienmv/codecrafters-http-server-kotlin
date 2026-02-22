package application

import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpContentEncoding
import domain.vo.HttpContentType

class HttpContext(val request: HttpRequest) {

    fun resultOK(status: HttpStatus = HttpStatus.OK_200): HttpResponse {
        return build(status)
    }

    fun resultText(text: String): HttpResponse {
        val responseHeaders = mutableMapOf("Content-Type" to "${HttpContentType.TEXT.value}; charset=utf-8")
        return build(
            status = HttpStatus.OK_200,
            responseHeaders = responseHeaders,
            body = text.toByteArray(Charsets.UTF_8)
        )
    }

    fun resultBytes(content: ByteArray): HttpResponse {
        val responseHeaders = mutableMapOf("Content-Type" to HttpContentType.OCTET_STREAM.value)
        return build(
            status = HttpStatus.OK_200,
            responseHeaders = responseHeaders,
            body = content
        )
    }

    fun resultError(status: HttpStatus): HttpResponse {
        val responseHeaders = mutableMapOf("Content-Type" to "${HttpContentType.TEXT.value}; charset=utf-8")
        return build(
            status = status,
            responseHeaders = responseHeaders,
            body = status.message.toByteArray(Charsets.UTF_8)
        )
    }

    private fun build(
        status: HttpStatus,
        responseHeaders: MutableMap<String, String> = mutableMapOf(),
        body: ByteArray? = null
    ): HttpResponse {
        if (request.headers["Connection"]?.lowercase() == "close") {
            responseHeaders["Connection"] = "close"
        }
        val acceptEncodings = request.headers["Accept-Encoding"] ?: ""
        val acceptEncodingSet = acceptEncodings.split(",").map { it.trim() }.toSet()
        val supportedEncoding = SUPPORTED_ENCODINGS.firstOrNull { acceptEncodingSet.contains(it) }
        if (body != null && supportedEncoding != null) {
            responseHeaders["Content-Encoding"] = supportedEncoding
        }

        return HttpResponse(
            status = status,
            headers = responseHeaders.toMap(),
            body = body
        )
    }

    companion object {
        private val SUPPORTED_ENCODINGS = listOf(HttpContentEncoding.GZIP.value)
    }
}