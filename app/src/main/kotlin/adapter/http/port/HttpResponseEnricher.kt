package adapter.http.port

import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse

interface HttpResponseEnricher {
    fun enrich(
        request: HttpRequest,
        response: HttpResponse,
    ): HttpResponse
}
