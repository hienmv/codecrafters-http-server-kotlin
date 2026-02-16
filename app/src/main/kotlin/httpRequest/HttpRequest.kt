package httpRequest

import common.HttpProtocol
import java.io.InputStream

data class HttpRequest(
    val method: HttpMethod,
    val requestTarget: String,
    val protocol: HttpProtocol,
    val requestHeaders: Map<String, String> = emptyMap(),
    val body: String,
) {
    companion object {
        fun parse(inputStream: InputStream): HttpRequest? {
            val reader = inputStream.bufferedReader()
            return try {
                val requestLine = reader.readLine() ?: return null

                val headerLines = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null && !line.isNullOrEmpty()) {
                    headerLines.add(line)
                }
                parse(
                    requestLine = requestLine,
                    headerLines = headerLines,
                    bodyLine = "" // TODO later,
                )
            } catch (e: Exception) {
                null
            }
        }

        fun parse(requestLine: String, headerLines: List<String>, bodyLine: String): HttpRequest? {
            return try {
                val requestLineParts = requestLine.split(" ")
                if (requestLineParts.size != 3) {
                    return null
                }
                val (method, requestTarget, protocol) = requestLineParts

                val requestHeaders = headerLines.associate { headerLine ->
                    val (key, value) = headerLine.split(":", limit = 2)
                    key.trim() to value.trim()
                }

                HttpRequest(
                    method = HttpMethod.valueOf(method),
                    requestTarget = requestTarget,
                    protocol = HttpProtocol.fromValue(protocol),
                    requestHeaders = requestHeaders,
                    body = bodyLine
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
