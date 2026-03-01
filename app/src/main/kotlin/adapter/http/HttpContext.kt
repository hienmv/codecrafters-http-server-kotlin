package adapter.http

import adapter.http.port.HttpRequestContext
import adapter.http.port.HttpResponseBuilder
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus

class HttpContext(
    override val request: HttpRequest,
    private val pathParams: Map<String, String> = mapOf(),
) : HttpRequestContext,
    HttpResponseBuilder {
    override fun pathParam(name: String): String = pathParams[name] ?: ""

    override fun result(status: HttpStatus) = HttpResponseFactory.empty(status)

    override fun resultText(text: String) = HttpResponseFactory.text(text)

    override fun resultBytes(content: ByteArray) = HttpResponseFactory.bytes(content)
}
