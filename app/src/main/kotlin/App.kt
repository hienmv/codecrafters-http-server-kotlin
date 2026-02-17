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

fun main(args: Array<String>) {
    val directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."

    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    while (true) { // keep server running
        val client = serverSocket.accept()
        Thread { // initialize a platform thread
            client.use { handle(it, directoryPath) }
        }.start()
    }
}

fun handle(client: Socket, directoryPath: String) {
    val inputStream = client.getInputStream()
    val outputStream = client.getOutputStream()
    try {
        // keep the loop running as long as the socket is open and not shut down
        // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
        while (!client.isClosed && !client.isInputShutdown) {
            val request = HttpRequest.parse(inputStream) ?: break
            val response = when {
                request.target == "/" -> getRoot()
                request.target.startsWith("/echo/") -> getEcho(request)
                request.target == "/user-agent" -> getUserAgent(request)
                request.target.startsWith("/files") ->
                    when (request.method) {
                        HttpMethod.GET -> getFile(request, directoryPath)
                        HttpMethod.POST -> appendFile(request, directoryPath)
                        else -> HttpResponse(status = HttpStatus.NOTFOUND_404)
                    }

                else -> HttpResponse(status = HttpStatus.NOTFOUND_404)
            }
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

fun getRoot() = HttpResponse(status = HttpStatus.OK_200)

fun getEcho(request: HttpRequest): HttpResponse {
    val acceptEncodings = request.headers["Accept-Encoding"]
    val responseContent = request.target.removePrefix("/echo/")
    return if (acceptEncodings != null) {
        val acceptEncodingSet = acceptEncodings.split(",").map { it.trim() }.toSet()
        val supportedEncoding = Constants.SUPPORTED_ENCODINGS.firstOrNull { acceptEncodingSet.contains(it) }
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.TEXT,
            contentEncoding = supportedEncoding,
            content = responseContent
        )
    } else {
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.TEXT,
            content = responseContent
        )
    }
}

fun getUserAgent(request: HttpRequest) = HttpResponse(
    status = HttpStatus.OK_200,
    contentType = HttpContentType.TEXT,
    content = request.headers["User-Agent"]
)

fun getFile(request: HttpRequest, directoryPath: String): HttpResponse {
    val fileName = request.target.removePrefix("/files/")
    val filePath = "$directoryPath/$fileName"
    return try {
        val content = File(filePath).readText()
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.OCTET_STREAM,
            content = content
        )
    } catch (e: IOException) {
        println(e.message)
        HttpResponse(status = HttpStatus.NOTFOUND_404)
    }
}

fun appendFile(request: HttpRequest, directoryPath: String): HttpResponse {
    val fileName = request.target.removePrefix("/files/")
    val filePath = "$directoryPath/$fileName"
    return try {
        val file = File(filePath)
        file.appendText(request.body)
        HttpResponse(status = HttpStatus.CREATED)
    } catch (e: IOException) {
        println(e.message)
        HttpResponse(status = HttpStatus.BAD_REQUEST)
    }
}