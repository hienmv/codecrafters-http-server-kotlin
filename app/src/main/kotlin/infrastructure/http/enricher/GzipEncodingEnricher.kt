package infrastructure.http.enricher

import adapter.http.port.HttpResponseEnricher
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse
import domain.vo.HttpContentEncoding

object GzipEncodingEnricher : HttpResponseEnricher {
    override fun enrich(
        request: HttpRequest,
        response: HttpResponse,
    ): HttpResponse {
        if (response.body == null) return response
        val acceptEncodings = request.headers["Accept-Encoding"] ?: return response
        val acceptEncodingSet = acceptEncodings.split(",").map { it.trim() }.toSet()
        if (!acceptEncodingSet.contains(HttpContentEncoding.GZIP.value)) return response
        val updatedHeaders = response.headers + ("Content-Encoding" to HttpContentEncoding.GZIP.value)
        return response.copy(headers = updatedHeaders)
    }
}
