import common.HttpContentType
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
    } else if (request?.requestTarget?.startsWith("/files") == true) {
        val fileName = request.requestTarget.removePrefix("/files/")
        val filePath = "$directoryPath/$fileName"
        try {
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
    } else {
        HttpResponse(status = HttpStatus.NOTFOUND_404)
    }
    outputStream.write(response.toString().toByteArray())
    outputStream.flush()
}
