package infrastructure.http.error

import adapter.http.HttpResponseFactory
import adapter.http.port.HttpErrorHandler
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

object NotFoundErrorHandler: HttpErrorHandler {
    override fun handle(t: Throwable): HttpResponse {
        return HttpResponseFactory.error(
            status = HttpStatus.NOT_FOUND_404,
            message = "The requested resource was not found",
        )
    }
}