package infrastructure.http

import domain.exception.PayloadTooLargeException
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.vo.HttpProtocol
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

object HttpRequestParser {
    // HTTP lines are terminated by \r\n (CRLF)
    // Returns null only if the stream ends before any byte is read (clean client disconnect)
    private fun readLine(stream: BufferedInputStream): String? {
        val bytes = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = stream.read()
            if (b == -1) {
                // end of stream: null if nothing was read yet, otherwise return what we have
                return if (bytes.size() == 0 && prev == -1) {
                    null
                } else {
                    bytes.toString(Charsets.ISO_8859_1)
                }
            }
            if (prev == '\r'.code && b == '\n'.code) {
                // strip the trailing \r and return the line
                val result = bytes.toByteArray()
                return String(result, 0, result.size - 1, Charsets.ISO_8859_1)
            }
            bytes.write(b)
            prev = b
        }
    }

    fun parse(
        stream: BufferedInputStream,
        maxRequestBodyBytes: Int,
    ): HttpRequest? {
        // request line
        val requestLine = readLine(stream) ?: return null
        val requestLineParts = requestLine.split(" ", limit = 3)
        if (requestLineParts.size != 3) {
            throw IllegalArgumentException("Unexpected HTTP request: $requestLine")
        }
        val (method, target, protocol) = requestLineParts
        val httpMethod =
            try {
                HttpMethod.valueOf(method)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unsupported HTTP method: $method")
            }
        val httpProtocol =
            HttpProtocol.fromValue(protocol)
                ?: throw IllegalArgumentException("Unsupported HTTP protocol: $protocol")

        // headers
        val headerLines = mutableListOf<String>()
        while (true) {
            val line = readLine(stream) ?: break // stream closed mid-headers
            if (line.isEmpty()) break // blank line = end of headers
            headerLines.add(line)
        }
        val headers =
            headerLines.associate { headerLine ->
                val parts = headerLine.split(":", limit = 2)
                if (parts.size != 2) throw IllegalArgumentException("Malformed header line: '$headerLine'")
                val (key, value) = parts
                key.trim() to value.trim()
            }

        // body — read directly into ByteArray, no charset conversion needed
        val contentLength = headers["Content-Length"]?.toInt() ?: 0
        if (contentLength < 0) {
            throw IllegalArgumentException("Invalid Content-Length: $contentLength")
        }
        if (contentLength > maxRequestBodyBytes) {
            throw PayloadTooLargeException(maxRequestBodyBytes)
        }
        val body =
            if (contentLength > 0) {
                val bodyBuffer = ByteArray(contentLength)
                var offset = 0
                while (offset < contentLength) {
                    val n = stream.read(bodyBuffer, offset, contentLength - offset)
                    if (n == -1) break
                    offset += n
                }
                if (offset != contentLength) {
                    throw IllegalArgumentException("Expected Content-Length of $contentLength but only read $offset bytes")
                }
                bodyBuffer
            } else {
                byteArrayOf()
            }

        return HttpRequest(
            method = httpMethod,
            target = target,
            protocol = httpProtocol,
            headers = headers,
            body = body,
        )
    }
}
