package adapter.http.handler

import adapter.http.HttpContext
import domain.httpResponse.HttpResponse

object UserAgentHandler {
    fun get(ctx: HttpContext): HttpResponse = ctx.resultText(ctx.request.headers["User-Agent"] ?: "Unknown")
}