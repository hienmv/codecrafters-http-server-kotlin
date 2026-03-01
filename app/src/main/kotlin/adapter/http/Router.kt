package adapter.http

import adapter.http.port.HttpResponseEnricher
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import java.net.URLDecoder

class Router(
    private val enrichers: List<HttpResponseEnricher> = emptyList(),
) {
    private val routes = mutableListOf<Route>()

    fun get(
        path: String,
        handler: (HttpContext) -> HttpResponse,
    ): Router {
        routes.add(Route(HttpMethod.GET, path, handler))
        return this
    }

    fun post(
        path: String,
        handler: (HttpContext) -> HttpResponse,
    ): Router {
        routes.add(Route(HttpMethod.POST, path, handler))
        return this
    }

    fun dispatch(request: HttpRequest): HttpResponse {
        val response =
            resolveRoute(request)?.let { match ->
                val ctx = HttpContext(request, pathParams = match.second)
                match.first.handler(ctx)
            } ?: HttpResponseFactory.error(HttpStatus.NOT_FOUND_404)

        return enrichers.fold(response) { acc, enricher ->
            enricher.enrich(request, acc)
        }
    }

    private fun resolveRoute(request: HttpRequest): Pair<Route, Map<String, String>>? {
        for (route in routes) {
            if (route.method != request.method) continue
            val pathParams = matchPath(route.path, request.target) ?: continue
            return route to pathParams
        }
        return null
    }

    private fun matchPath(
        pattern: String,
        path: String,
    ): Map<String, String>? {
        val patternSegments = pattern.split("/")
        val pathSegments = path.split("/")
        if (patternSegments.size != pathSegments.size) return null
        val pathParams = mutableMapOf<String, String>()
        for ((patternSegment, pathSegment) in patternSegments.zip(pathSegments)) {
            if (patternSegment.startsWith("{") && patternSegment.endsWith("}")) {
                val paramName = patternSegment.substring(1, patternSegment.length - 1)
                pathParams[paramName] = URLDecoder.decode(pathSegment, Charsets.UTF_8)
            } else if (patternSegment != pathSegment) {
                return null
            }
        }

        return pathParams
    }
}

internal data class Route(
    val method: HttpMethod,
    val path: String,
    val handler: (HttpContext) -> HttpResponse,
)
