package adapter.http

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpContentType

object HttpResponseFactory {
    fun empty(status: HttpStatus): HttpResponse {
        return HttpResponse(
            status = status,
            headers = mapOf(),
        )
    }

    fun text(text: String): HttpResponse {
        val responseHeaders = mapOf("Content-Type" to "${HttpContentType.TEXT.value}; charset=utf-8")
        return HttpResponse(
            status = HttpStatus.OK_200,
            headers = responseHeaders,
            body = text.toByteArray(Charsets.UTF_8)
        )
    }
    fun bytes(content: ByteArray): HttpResponse {
        val responseHeaders = mapOf("Content-Type" to HttpContentType.OCTET_STREAM.value)
        return HttpResponse(
            status = HttpStatus.OK_200,
            headers = responseHeaders,
            body = content
        )
    }
    fun error(status: HttpStatus, message: String? = null): HttpResponse = HttpResponse(
        status = status,
        headers = mapOf("Content-Type" to "text/plain; charset=utf-8"),
        body = message?.toByteArray(Charsets.UTF_8) ?: status.message.toByteArray(Charsets.UTF_8),
    )
}