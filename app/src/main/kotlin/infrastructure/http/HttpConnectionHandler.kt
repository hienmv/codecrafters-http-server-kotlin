package infrastructure.http

import adapters.HttpController
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpContentType
import java.io.BufferedInputStream
import java.net.Socket

class HttpConnectionHandler(private val httpController: HttpController) {
    fun handle(socket: Socket) {
        val stream = BufferedInputStream(socket.getInputStream())
        val socketResponseWriter = SocketResponseWriter(socket)
        try {
            // keep the loop running as long as the socket is open and not shut down
            // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
            while (!socket.isClosed && !socket.isInputShutdown) {
                val request = HttpRequestParser.parse(stream) ?: break
                httpController.route(request, socketResponseWriter)
                // client explicitly asks to close
                if (request.headers["Connection"]?.lowercase() == "close") {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            socketResponseWriter.writeResponse(
                HttpResponse(
                    status = HttpStatus.BAD_REQUEST_400,
                    headers = mapOf("Content-Type" to "${HttpContentType.TEXT.value}; charset=utf-8"),
                    body = "Bad Request: ${e.message}".toByteArray(Charsets.UTF_8)
                )
            )
        }
    }
}