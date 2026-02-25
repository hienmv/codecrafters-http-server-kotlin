package infrastructure.http.error

import adapter.http.HttpResponseFactory
import adapter.http.port.HttpErrorHandler
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

object InvalidRequestErrorHandler: HttpErrorHandler {
    override fun handle(t: Throwable): HttpResponse {
        return HttpResponseFactory.error(
            status = HttpStatus.BAD_REQUEST_400,
            message = t.message,
        )
    }
}