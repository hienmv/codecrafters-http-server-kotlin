package adapter.http.handler

import adapter.http.HttpContext
import application.usecase.GetFileContent
import domain.httpResponse.HttpResponse

class GetFileContentHandler(
    private val getFileUserCase: GetFileContent,
) {
    fun get(ctx: HttpContext): HttpResponse {
        val fileName = ctx.pathParam("fileName")
        return ctx.resultBytes(getFileUserCase.execute(fileName))
    }
}
