package adapter.http.handler

import adapter.http.HttpContext
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

object RootHandler {
    fun get(ctx: HttpContext): HttpResponse = ctx.result(HttpStatus.OK_200)
}
