import common.HttpContentType
import httpRequest.HttpRequest
import httpResponse.HttpResponse
import httpResponse.HttpStatus
import java.net.ServerSocket

fun main() {
    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    while (true) { // keep server running
        serverSocket.accept().use { client ->
            val outputStream = client.getOutputStream()
            val request = HttpRequest.parse(client.getInputStream())
            val response = if (request?.requestTarget == "/") {
                HttpResponse(status = HttpStatus.OK_200)
            } else if (request?.requestTarget?.startsWith("/echo/") == true) {
                HttpResponse(
                    status = HttpStatus.OK_200,
                    contentType = HttpContentType.TEXT,
                    content = request.requestTarget.removePrefix("/echo/")
                )
            } else if (request?.requestTarget == "/user-agent") {
                HttpResponse(
                    status = HttpStatus.OK_200,
                    contentType = HttpContentType.TEXT,
                    content = request.requestHeaders["User-Agent"]
                )
            } else {
                HttpResponse(status = HttpStatus.NOTFOUND_404)
            }
            outputStream.write(response.toString().toByteArray())
            outputStream.flush()
        }
    }
}
