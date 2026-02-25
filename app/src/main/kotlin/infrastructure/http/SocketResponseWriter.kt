package infrastructure.http

import adapter.http.port.ResponseWriter
import domain.httpResponse.HttpResponse
import java.net.Socket

class SocketResponseWriter(socket: Socket) : ResponseWriter {
    private val outputStream = socket.getOutputStream()
    override fun writeResponse(response: HttpResponse) {
        // TODO: try-catch and handle IOException, e.g. client disconnects before we can write the response
        // TODO: if the response is too big, we should stream the response body instead of loading it all in memory
        outputStream.write(HttpResponseSerializer.serialize(response))
        outputStream.flush()
    }
}