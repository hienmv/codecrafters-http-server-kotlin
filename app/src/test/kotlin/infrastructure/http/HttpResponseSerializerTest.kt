package infrastructure.http

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class HttpResponseSerializerTest :
    DescribeSpec({

        describe("HttpResponseSerializer") {

            it("serializes the status line correctly") {
                // Arrange
                val response = HttpResponse(status = HttpStatus.OK_200, protocol = HttpProtocol.HTTP11)
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "HTTP/1.1 200 OK\r\n"
            }

            it("includes all response headers in the output") {
                // Arrange
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("Content-Type" to "text/plain"),
                    )
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "Content-Type: text/plain\r\n"
            }

            it("adds Content-Length header equal to body byte size") {
                // Arrange
                val response = HttpResponse(status = HttpStatus.OK_200, body = "hello".toByteArray())
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "Content-Length: 5\r\n"
            }

            it("adds Content-Length: 0 when body is null") {
                // Arrange
                val response = HttpResponse(status = HttpStatus.OK_200, body = null)
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "Content-Length: 0\r\n"
            }

            it("gzip-encodes the body when Content-Encoding: gzip header is present") {
                // Arrange
                val originalBody = "compress me".toByteArray()
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("Content-Encoding" to "gzip"),
                        body = originalBody,
                    )
                // Act
                val serialized = HttpResponseSerializer.serialize(response)
                val headerEnd = findHeaderBodySeparator(serialized)
                val bodyBytes = serialized.copyOfRange(headerEnd, serialized.size)
                val decompressed = GZIPInputStream(ByteArrayInputStream(bodyBytes)).readBytes()
                // Assert
                decompressed.toList() shouldBe originalBody.toList()
            }

            it("passes a non-gzip body through without modification") {
                // Arrange
                val body = byteArrayOf(1, 2, 3)
                val response = HttpResponse(status = HttpStatus.OK_200, body = body)
                // Act
                val serialized = HttpResponseSerializer.serialize(response)
                val headerEnd = findHeaderBodySeparator(serialized)
                val bodyBytes = serialized.copyOfRange(headerEnd, serialized.size)
                // Assert
                bodyBytes.toList() shouldBe body.toList()
            }

            it("strips CRLF from header value to prevent response splitting") {
                // Arrange
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("X-Header" to "legitimate\r\nSet-Cookie: injected"),
                    )
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "X-Header: legitimateSet-Cookie: injected\r\n"
            }

            it("strips CR alone from header value") {
                // Arrange
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("X-Header" to "value\rwith\rcr"),
                    )
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "X-Header: valuewithcr\r\n"
            }

            it("strips LF alone from header value") {
                // Arrange
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("X-Header" to "value\nwith\nlf"),
                    )
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "X-Header: valuewithlf\r\n"
            }

            it("strips CRLF from header key") {
                // Arrange
                val response =
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("X-Head\r\ner" to "value"),
                    )
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "X-Header: value\r\n"
            }

            it("separates headers and body with a blank CRLF line") {
                // Arrange
                val response = HttpResponse(status = HttpStatus.OK_200, body = "data".toByteArray())
                // Act
                val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)
                // Assert
                serialized shouldContain "\r\n\r\n"
            }
        }
    })

private fun findHeaderBodySeparator(bytes: ByteArray): Int {
    for (i in 0..bytes.size - 4) {
        if (bytes[i] == '\r'.code.toByte() &&
            bytes[i + 1] == '\n'.code.toByte() &&
            bytes[i + 2] == '\r'.code.toByte() &&
            bytes[i + 3] == '\n'.code.toByte()
        ) {
            return i + 4
        }
    }
    return bytes.size
}
