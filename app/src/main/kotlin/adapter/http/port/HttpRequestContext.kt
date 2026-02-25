package adapter.http.port

import domain.httpRequest.HttpRequest

interface HttpRequestContext {
    val request: HttpRequest
    fun pathParam(name: String): String
}