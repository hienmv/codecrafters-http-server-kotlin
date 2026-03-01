package infrastructure.http

import adapter.http.port.ResponseWriter
import domain.httpResponse.HttpResponse
import java.io.IOException
import java.net.Socket

class SocketResponseWriter(
    socket: Socket,
) : ResponseWriter {
    private val outputStream = socket.getOutputStream()

    override fun writeResponse(response: HttpResponse) {
        // TODO: if the response is too big, we should stream the response body instead of loading it all in memory
        try {
            outputStream.write(HttpResponseSerializer.serialize(response))
            outputStream.flush()
        } catch (e: IOException) {
            // Log the error and close the socket
            println("Error writing response: ${e.message}")
            outputStream.close()
        }
    }
}
