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
    val outputStream = client.getOutputStream()
    val request = HttpRequest.parse(client.getInputStream())
    val response = if (request?.target == "/") {
        getRoot()
    } else if (request?.target?.startsWith("/echo/") == true) {
        getEcho(request)
    } else if (request?.target == "/user-agent") {
        getUserAgent(request)
    } else if (request?.method == HttpMethod.GET && request.target.startsWith("/files")) {
        getFile(request, directoryPath)
    } else if (request?.method == HttpMethod.POST && request.target.startsWith("/files")) {
        appendFile(request, directoryPath)
    } else {
        HttpResponse(status = HttpStatus.NOTFOUND_404)
    }
    outputStream.write(response.toString().toByteArray())
    outputStream.flush()
}

fun getRoot() = HttpResponse(status = HttpStatus.OK_200)

fun getEcho(request: HttpRequest): HttpResponse {
    val acceptEncodings = request.headers["Accept-Encoding"]
    return if (acceptEncodings != null) {
        val acceptEncodingSet = acceptEncodings.split(",").map { it.trim() }.toSet()
        val supportedEncoding = Constants.SUPPORTED_ENCODINGS.firstOrNull { acceptEncodingSet.contains(it) }
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.TEXT,
            contentEncoding = supportedEncoding,
        )
    } else {
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.TEXT,
            body = request.target.removePrefix("/echo/")
        )
    }
}

fun getUserAgent(request: HttpRequest) = HttpResponse(
    status = HttpStatus.OK_200,
    contentType = HttpContentType.TEXT,
    body = request.headers["User-Agent"]
)

fun getFile(request: HttpRequest, directoryPath: String): HttpResponse {
    val fileName = request.target.removePrefix("/files/")
    val filePath = "$directoryPath/$fileName"
    return try {
        val content = File(filePath).readText()
        HttpResponse(
            status = HttpStatus.OK_200,
            contentType = HttpContentType.OCTET_STREAM,
            body = content
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