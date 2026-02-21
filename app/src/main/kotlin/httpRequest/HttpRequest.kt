package httpRequest

import common.HttpProtocol
import java.io.BufferedReader

data class HttpRequest(
    val method: HttpMethod,
    val target: String,
    val protocol: HttpProtocol,
    val headers: Map<String, String> = emptyMap(),
    val body: String,
) {
    companion object {
        fun parse(bufferedReader: BufferedReader): HttpRequest? {
            return try {
                // request line
                val requestLine = bufferedReader.readLine() ?: return null
                val requestLineParts = requestLine.split(" ", limit = 3)
                val (method, target, protocol) = requestLineParts

                // headers
                val headerLines = mutableListOf<String>()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null && !line.isNullOrEmpty()) {
                    headerLines.add(line)
                }
                val headers = headerLines.associate { headerLine ->
                    val (key, value) = headerLine.split(":", limit = 2)
                    key.trim() to value.trim()
                }

                // body
                val contentLength = headers["Content-Length"]?.toInt() ?: 0
                val body = if (contentLength > 0) {
                    val charBuffer = CharArray(contentLength)
                    bufferedReader.read(charBuffer, 0, contentLength)
                    String(charBuffer)
                } else ""

                HttpRequest(
                    method = HttpMethod.valueOf(method),
                    target = target,
                    protocol = HttpProtocol.fromValue(protocol),
                    headers = headers,
                    body = body
                )
            } catch (e: Exception) {
                println(e.message)
                null
            }
        }
    }
}
