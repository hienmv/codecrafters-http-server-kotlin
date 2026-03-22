package infrastructure.http

import adapter.http.port.HttpAdapter
import adapter.http.port.HttpErrorHandler
import java.io.BufferedInputStream
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class HttpConnectionHandler(
    private val adapter: HttpAdapter,
    private val errorHandler: HttpErrorHandler,
    private val maxRequestBodyBytes: Int,
) {
    fun handle(socket: Socket) {
        val stream = BufferedInputStream(socket.getInputStream())
        val socketResponseWriter = SocketResponseWriter(socket)
        try {
            // keep the loop running as long as the socket is open and not shut down
            // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
            while (!socket.isClosed && !socket.isInputShutdown) {
                val request = HttpRequestParser.parse(stream, maxRequestBodyBytes) ?: break
                adapter.handle(request, socketResponseWriter)
                // client explicitly asks to close
                if (request.headers["Connection"]?.lowercase() == "close") {
                    break
                }
            }
        } catch (_: SocketTimeoutException) {
            // Slow client — timed out waiting for data; silently close the connection.
        } catch (_: SocketException) {
            // Client disconnected (connection reset); silently close.
        } catch (t: Throwable) {
            socketResponseWriter.writeResponse(errorHandler.handle(t))
        }
    }
}
