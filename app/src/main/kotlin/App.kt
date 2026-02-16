import java.net.ServerSocket;
import java.util.Scanner

fun main() {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    println("Logs from your program will appear here!")

    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    while (true) { // keep server running
        serverSocket.accept().use { client ->
            val input = Scanner(client.getInputStream())
            val outputStream = client.getOutputStream()

            val httpRequestLine = HttpRequestLine.parse(input.nextLine())
            val response =
                if (httpRequestLine?.path?.value == "/") {
                    HttpResponseMessage(status = HttpStatus.OK_200)
                } else if (httpRequestLine?.path?.value?.startsWith("/echo/") == true) {
                    val body = httpRequestLine.path.value.removePrefix("/echo/")
                    HttpResponseMessage(
                        status = HttpStatus.OK_200,
                        representationHeader = HttpRepresentationHeader(
                            contentType = HttpContentType.TEXT,
                            contentLength = body.toByteArray().size.toLong()
                        ),
                        body = body
                    )
                } else {
                    HttpResponseMessage(status = HttpStatus.NOTFOUND_404)
                }
            outputStream.write(response.toString().toByteArray())
            outputStream.flush()
        }
    }
}

enum class HttpMethod {
    GET, POST, PUT, DELETE
}

enum class HttpProtocol(val value: String) {
    HTTP1("HTTP/1.0"),
    HTTP11("HTTP/1.1");


    companion object {
        fun fromValue(value: String): HttpProtocol = entries.first { it.value == value }
    }
}

enum class HttpRequestTargetType {
    ORIGINAL_FORM,
    ABSOLUTE_FORM,
    AUTHORITY_FORM,
    ASTERISK_FORM
}

data class HttpRequestTarget(
    val type: HttpRequestTargetType,
    val value: String
) {
    init {
        // currently, accept only original form request target
        if (type == HttpRequestTargetType.ORIGINAL_FORM) {
            require(value.startsWith("/")) {
                throw Exception("invalid original form target")
            }
        } else {
            throw Exception("not support request target")
        }
    }
}

data class HttpRequestLine(
    val method: HttpMethod,
    val path: HttpRequestTarget,
    val protocol: HttpProtocol,
) {
    companion object {
        fun parse(line: String): HttpRequestLine? {
            val parts = line.split(" ")
            if (parts.size != 3) {
                return null
            }
            val (method, path, protocol) = parts
            return try {
                HttpRequestLine(
                    method = HttpMethod.valueOf(method),
                    path = HttpRequestTarget(
                        type = HttpRequestTargetType.ORIGINAL_FORM,
                        value = path
                    ),
                    protocol = HttpProtocol.fromValue(protocol)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class HttpStatus(val code: Int, val message: String) {
    OK_200(code = 200, message = "OK"),
    NOTFOUND_404(code = 404, message = "Not found")
}

enum class HttpContentType(val value: String) {
    JSON("application/json"),
    TEXT("text/plain"),
    HTML("text/html")
}

data class HttpRepresentationHeader(
    val contentType: HttpContentType,
    val contentLength: Long,
)

data class HttpResponseMessage(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val representationHeader: HttpRepresentationHeader? = null,
    val body: String? = null
) {
    override fun toString(): String {
        val crlf = "\r\n"
        val statusLine = listOf(protocol.value, status.code, status.message).joinToString(" ")
        val representationHeaderLines = representationHeader?.let {
            "Content-Type: ${it.contentType.value}${crlf}Content-Length: ${it.contentLength}"
        } ?: ""
        // Status + CRLF + Headers + CRLF + CRLF + Body
        return "$statusLine$crlf$representationHeaderLines$crlf$crlf${body ?: ""}"
    }
}