package infrastructure.http.error

import adapter.http.HttpResponseFactory
import adapter.http.port.HttpErrorHandler
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

object PayloadTooLargeRequestErrorHandler : HttpErrorHandler {
    override fun handle(t: Throwable): HttpResponse =
        HttpResponseFactory.error(
            status = HttpStatus.PAYLOAD_TOO_LARGE_413,
            message = t.message,
        )
}
