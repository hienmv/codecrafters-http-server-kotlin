package adapter.http

import adapter.http.port.HttpRequestContext
import adapter.http.port.HttpResponseBuilder
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus

class HttpContext(
    override val request: HttpRequest,
    private val pathParams: Map<String, String> = mapOf()
): HttpRequestContext, HttpResponseBuilder {
    override fun pathParam(name: String): String = pathParams[name] ?: ""

    override fun resultOK() =  HttpResponseFactory.status(HttpStatus.OK_200)
    override fun resultCreated() =  HttpResponseFactory.status(HttpStatus.CREATED_201)
    override fun resultText(text: String) = HttpResponseFactory.text(text)
    override fun resultBytes(content: ByteArray) = HttpResponseFactory.bytes(content)
    override fun resultError(status: HttpStatus) =  HttpResponseFactory.error(status)
}
