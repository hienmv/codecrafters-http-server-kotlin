package adapter.http.port

import domain.httpResponse.HttpResponse

interface HttpErrorHandler {
    fun handle(t: Throwable): HttpResponse
}