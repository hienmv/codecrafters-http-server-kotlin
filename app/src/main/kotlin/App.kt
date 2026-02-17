import common.Constants
import common.HttpContentType
import httpRequest.HttpMethod
import httpRequest.HttpRequest
import httpResponse.HttpResponse
import httpResponse.HttpStatus
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

object Config {
    var directoryPath = ""
}

fun main(args: Array<String>) {
    Config.directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."

    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    while (true) { // keep server running
        val client = serverSocket.accept()
        Thread { // initialize a platform thread
            client.use { handle(it) }
        }.start()
    }
}

fun handle(client: Socket) {
    val inputStream = client.getInputStream()
    val outputStream = client.getOutputStream()
    try {
        // keep the loop running as long as the socket is open and not shut down
        // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
        while (!client.isClosed && !client.isInputShutdown) {
            val request = HttpRequest.parse(inputStream) ?: break
            val response = handleRequest(request)
            outputStream.write(response.toBytes())
            outputStream.flush()

            // client explicitly asks to close
            if (request.headers["Connection"]?.lowercase() == "close") {
                break
            }
        }
    } catch (e: Exception) {
        println(e.printStackTrace())
    }
}

fun handleRequest(request: HttpRequest): HttpResponse {
    val responseHeaders = mutableMapOf<String, String>()
    if (request.headers["Connection"]?.lowercase() == "close") {
        responseHeaders.put("Connection", "close")
    }
    val acceptEncodings = request.headers["Accept-Encoding"] ?: ""
    val acceptEncodingSet = acceptEncodings.split(",").map { it.trim() }.toSet()
    val supportedEncoding = Constants.SUPPORTED_ENCODINGS.firstOrNull { acceptEncodingSet.contains(it) }
    if (supportedEncoding != null) {
        responseHeaders["Content-Encoding"] = supportedEncoding
    }

    var content: String? = null
    var status: HttpStatus
    when {
        request.target == "/" -> status = HttpStatus.OK_200
        request.target.startsWith("/echo/") -> {
            responseHeaders.put("Content-Type", HttpContentType.TEXT.value)
            status = HttpStatus.OK_200
            content = request.target.removePrefix("/echo/")
        }

        request.target == "/user-agent" -> {
            responseHeaders.put("Content-Type", HttpContentType.TEXT.value)
            status = HttpStatus.OK_200
            content = request.headers["User-Agent"]
        }

        request.target.startsWith("/files") ->
            when (request.method) {
                HttpMethod.GET -> {
                    responseHeaders.put("Content-Type", HttpContentType.OCTET_STREAM.value)
                    val fileName = request.target.removePrefix("/files/")
                    val filePath = "${Config.directoryPath}/$fileName"
                    try {
                        status = HttpStatus.OK_200
                        content = File(filePath).readText()
                    } catch (e: IOException) {
                        println(e.message)
                        status = HttpStatus.NOTFOUND_404
                    }
                }

                HttpMethod.POST -> {
                    val fileName = request.target.removePrefix("/files/")
                    val filePath = "${Config.directoryPath}/$fileName"
                    try {
                        val file = File(filePath)
                        file.appendText(request.body)
                        status = HttpStatus.CREATED
                    } catch (e: IOException) {
                        println(e.message)
                        status = HttpStatus.BAD_REQUEST
                    }
                }

                else -> status = HttpStatus.NOTFOUND_404
            }

        else -> status = HttpStatus.NOTFOUND_404
    }

    return HttpResponse(
        status = status,
        headers = responseHeaders,
        content = content
    )
}
