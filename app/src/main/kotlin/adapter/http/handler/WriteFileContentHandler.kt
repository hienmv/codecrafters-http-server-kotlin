package adapter.http.handler

import adapter.http.HttpContext
import application.usecase.WriteFileContent
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

class WriteFileContentHandler(private val writeFileContent: WriteFileContent) {
    fun create(ctx: HttpContext): HttpResponse {
        writeFileContent.execute(ctx.pathParam("fileName"), ctx.request.body)
        return ctx.result(HttpStatus.CREATED_201)
    }
}