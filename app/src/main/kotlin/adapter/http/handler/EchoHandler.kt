package adapter.http.handler

import adapter.http.HttpContext
import domain.httpResponse.HttpResponse

object EchoHandler {
    fun get(ctx: HttpContext): HttpResponse = ctx.resultText(ctx.pathParam("text"))
}
