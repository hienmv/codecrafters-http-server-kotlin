package infrastructure.http.enricher

import adapter.http.port.HttpResponseEnricher
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse

object ConnectionHeaderEnricher : HttpResponseEnricher {
    override fun enrich(request: HttpRequest, response: HttpResponse): HttpResponse {
        return if (request.headers["Connection"]?.lowercase() == "close") {
            response.copy(headers = response.headers + ("Connection" to "close"))
        } else response
    }
}