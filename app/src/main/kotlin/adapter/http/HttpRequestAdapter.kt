package adapter.http

import adapter.http.port.HttpAdapter
import adapter.http.port.ResponseWriter
import domain.httpRequest.HttpRequest

class HttpRequestAdapter(private val router: Router): HttpAdapter {
    override fun handle(request: HttpRequest, writer: ResponseWriter) {
        writer.writeResponse(router.dispatch(request))
    }
}