package adapters

import application.RequestDispatcher
import domain.httpRequest.HttpRequest

class HttpController(private val requestDispatcher: RequestDispatcher) {
    fun route(request: HttpRequest, writer: ResponseWriter) {
        writer.writeResponse(requestDispatcher.dispatch(request))
    }
}