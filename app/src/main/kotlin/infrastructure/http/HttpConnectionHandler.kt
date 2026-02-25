package infrastructure.http

import adapter.http.port.HttpAdapter
import adapter.http.port.HttpErrorHandler
import java.io.BufferedInputStream
import java.net.Socket

class HttpConnectionHandler(
    private val adapter: HttpAdapter,
    private val errorHandler: HttpErrorHandler
) {
    fun handle(socket: Socket) {
        val stream = BufferedInputStream(socket.getInputStream())
        val socketResponseWriter = SocketResponseWriter(socket)
        try {
            // keep the loop running as long as the socket is open and not shut down
            // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
            while (!socket.isClosed && !socket.isInputShutdown) {
                val request = HttpRequestParser.parse(stream) ?: break
                adapter.handle(request, socketResponseWriter)
                // client explicitly asks to close
                if (request.headers["Connection"]?.lowercase() == "close") {
                    break
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            socketResponseWriter.writeResponse(errorHandler.handle(t))
        }
    }
}