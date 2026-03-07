package infrastructure.http

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import java.net.Socket

object HttpConnectionLimitHandler {
    fun reject(socket: Socket) {
        val response =
            HttpResponse(
                status = HttpStatus.SERVICE_UNAVAILABLE_503,
                headers = mapOf("Connection" to "close"),
            )
        socket.getOutputStream().write(HttpResponseSerializer.serialize(response))
        socket.close()
    }
}
