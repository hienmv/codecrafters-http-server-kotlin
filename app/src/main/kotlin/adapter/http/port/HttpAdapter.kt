package adapter.http.port

import domain.httpRequest.HttpRequest

interface HttpAdapter {
    fun handle(
        request: HttpRequest,
        writer: ResponseWriter,
    )
}
