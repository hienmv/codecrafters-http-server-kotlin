package infrastructure.http.error

import adapter.http.HttpResponseFactory
import adapter.http.port.HttpErrorHandler
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

object FallbackErrorHandler: HttpErrorHandler {
    override fun handle(t: Throwable): HttpResponse {
        t.printStackTrace()
        return HttpResponseFactory.error(
            status = HttpStatus.SERVER_ERROR_500,
            message = "Internal Server Error"
        )
    }
}