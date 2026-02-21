import common.Constants
import common.HttpContentType
import httpRequest.HttpMethod
import httpRequest.HttpRequest
import httpResponse.HttpResponse
import httpResponse.HttpStatus
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

object Config {
    lateinit var directoryPath: String
        private set

    fun initialize(directoryPath: String) {
        check(!::directoryPath.isInitialized) { "Config can only be initialized once" }
        this.directoryPath = directoryPath
    }
}

fun main(args: Array<String>) {
    val directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."
    Config.initialize(directoryPath)

    val serverSocket = ServerSocket()
    serverSocket.reuseAddress = true
    serverSocket.bind(InetSocketAddress(4221))

    while (true) { // keep server running
        val client = serverSocket.accept()
        Thread { // initialize a platform thread
            client.use { handle(it) }
        }.start()
    }
}

fun handle(client: Socket) {
    // one reader per connection to no data loss when handling persistent HTTP Connections (keep-alive)
    // bufferReader will read from the socket input stream in chunks
    // and keep the remaining data in memory until the next read,
    // so we won't lose any data when parsing multiple HTTP requests from the same connection
    val bufferedReader = client.getInputStream().bufferedReader()
    val outputStream = client.getOutputStream()
    try {
        // keep the loop running as long as the socket is open and not shut down
        // keep persistent HTTP Connections: the same TCP Connection can be reused for multiple requests
        while (!client.isClosed && !client.isInputShutdown) {
            try {
                val request = HttpRequest.parse(bufferedReader) ?: break
                val response = handleRequest(request)
                outputStream.write(response.toBytes())
                outputStream.flush()

                // client explicitly asks to close
                if (request.headers["Connection"]?.lowercase() == "close") {
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
                outputStream.write(HttpResponse(status = HttpStatus.BAD_REQUEST_400, content = "Bad Request: ${e.message}").toBytes())
                outputStream.flush()
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun handleRequest(request: HttpRequest): HttpResponse {
    val responseHeaders = mutableMapOf<String, String>()
    if (request.headers["Connection"]?.lowercase() == "close") {
        responseHeaders["Connection"] = "close"
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
            responseHeaders["Content-Type"] = HttpContentType.TEXT.value
            status = HttpStatus.OK_200
            content = request.target.removePrefix("/echo/")
        }

        request.target == "/user-agent" -> {
            responseHeaders["Content-Type"] = HttpContentType.TEXT.value
            status = HttpStatus.OK_200
            content = request.headers["User-Agent"]
        }

        request.target.startsWith("/files/") -> when (request.method) {
            HttpMethod.GET -> {
                responseHeaders["Content-Type"] = HttpContentType.OCTET_STREAM.value
                val fileName = request.target.removePrefix("/files/")
                val basePath = File(Config.directoryPath).canonicalPath
                val filePath = File(Config.directoryPath, fileName).canonicalPath
                status = if (!filePath.startsWith(basePath + File.separator)) {
                    HttpStatus.NOT_FOUND_404
                } else {
                    try {
                        // TODO: not for big files, we should stream the file content to the response instead of loading it all in memory
                        content = File(filePath).readText()
                        HttpStatus.OK_200
                    } catch (e: IOException) {
                        println(e.message)
                        HttpStatus.NOT_FOUND_404
                    }
                }
            }

            HttpMethod.POST -> {
                val fileName = request.target.removePrefix("/files/")
                val basePath = File(Config.directoryPath).canonicalPath
                val filePath = File(Config.directoryPath, fileName).canonicalPath
                status = if (!filePath.startsWith(basePath + File.separator)) {
                    HttpStatus.BAD_REQUEST_400
                } else {
                    try {
                        // TODO: not for big files, we should stream the request body to the file instead of loading it all in memory
                        val file = File(filePath)
                        // NOT append, since POST semantic is to create a new resource, if the file already exists, we should overwrite it
                        file.writeText(request.body)
                        HttpStatus.CREATED_201
                    } catch (e: IOException) {
                        println(e.message)
                        HttpStatus.BAD_REQUEST_400
                    }
                }
            }

            else -> status = HttpStatus.NOT_FOUND_404
        }

        else -> status = HttpStatus.NOT_FOUND_404
    }

    return HttpResponse(
        status = status, headers = responseHeaders, content = content
    )
}
