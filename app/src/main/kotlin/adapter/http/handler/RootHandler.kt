package adapter.http.handler

import adapter.http.HttpContext
import domain.httpResponse.HttpResponse

object RootHandler {
    fun get(ctx: HttpContext): HttpResponse = ctx.resultOK()
}