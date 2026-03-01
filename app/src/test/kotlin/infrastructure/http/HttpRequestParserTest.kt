package infrastructure.http

import domain.httpRequest.HttpMethod
import domain.vo.HttpProtocol
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

class HttpRequestParserTest : DescribeSpec({

    fun makeStream(raw: String) =
        BufferedInputStream(ByteArrayInputStream(raw.toByteArray(Charsets.ISO_8859_1)))

    fun makeStream(bytes: ByteArray) =
        BufferedInputStream(ByteArrayInputStream(bytes))

    describe("HttpRequestParser") {

        it("parses a minimal GET request with no headers and no body") {
            // Arrange
            val raw = "GET / HTTP/1.1\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.method shouldBe HttpMethod.GET
            request.target shouldBe "/"
            request.protocol shouldBe HttpProtocol.HTTP11
            request.headers shouldBe emptyMap()
            request.body.size shouldBe 0
        }

        it("parses all headers as key-value pairs") {
            // Arrange
            val raw = "GET / HTTP/1.1\r\nHost: localhost\r\nUser-Agent: TestClient\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.headers["Host"] shouldBe "localhost"
            request.headers["User-Agent"] shouldBe "TestClient"
        }

        it("parses POST body according to Content-Length") {
            // Arrange
            val body = "hello"
            val raw = "POST /submit HTTP/1.1\r\nContent-Length: ${body.length}\r\n\r\n$body"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            String(request!!.body, Charsets.UTF_8) shouldBe body
        }

        it("returns null on an empty stream (clean client disconnect)") {
            // Arrange
            val stream = BufferedInputStream(ByteArrayInputStream(byteArrayOf()))
            // Act
            val result = HttpRequestParser.parse(stream)
            // Assert
            result.shouldBeNull()
        }

        it("strips carriage returns so header values contain no trailing CR") {
            // Arrange
            val raw = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.headers["Host"] shouldBe "localhost"
        }

        it("parses HTTP/1.0 protocol correctly") {
            // Arrange
            val raw = "GET / HTTP/1.0\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.protocol shouldBe HttpProtocol.HTTP1
        }

        it("handles binary body bytes without corruption") {
            // Arrange
            val bodyBytes = byteArrayOf(0xFF.toByte(), 0x00.toByte(), 0xAB.toByte())
            val header = "POST /upload HTTP/1.1\r\nContent-Length: ${bodyBytes.size}\r\n\r\n"
                .toByteArray(Charsets.ISO_8859_1)
            val fullBytes = header + bodyBytes
            // Act
            val request = HttpRequestParser.parse(makeStream(fullBytes))
            // Assert
            request!!.body.toList() shouldBe bodyBytes.toList()
        }

        it("produces an empty body when Content-Length is zero") {
            // Arrange
            val raw = "POST /submit HTTP/1.1\r\nContent-Length: 0\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.body.size shouldBe 0
        }

        it("produces an empty body when Content-Length header is absent") {
            // Arrange
            val raw = "GET /items HTTP/1.1\r\n\r\n"
            // Act
            val request = HttpRequestParser.parse(makeStream(raw))
            // Assert
            request!!.body.size shouldBe 0
        }

        it("throws IllegalArgumentException for a request line with fewer than 3 parts") {
            // Arrange
            val raw = "GET /\r\n\r\n" // missing protocol
            // Act & Assert
            shouldThrow<IllegalArgumentException> {
                HttpRequestParser.parse(makeStream(raw))
            }
        }

        it("throws IllegalArgumentException for an empty request line") {
            // Arrange
            val raw = "\r\n\r\n"
            // Act & Assert
            shouldThrow<IllegalArgumentException> {
                HttpRequestParser.parse(makeStream(raw))
            }
        }

        it("throws IllegalArgumentException for an unknown HTTP method") {
            // Arrange
            val raw = "INVALID / HTTP/1.1\r\n\r\n"
            // Act & Assert
            shouldThrow<IllegalArgumentException> {
                HttpRequestParser.parse(makeStream(raw))
            }
        }

        it("throws IllegalArgumentException for an unknown HTTP protocol") {
            // Arrange
            val raw = "GET / HTTP/9.9\r\n\r\n"
            // Act & Assert
            shouldThrow<IllegalArgumentException> {
                HttpRequestParser.parse(makeStream(raw))
            }
        }

        it("throws IllegalArgumentException for a malformed header line with no colon") {
            // Arrange
            val raw = "GET / HTTP/1.1\r\nBadHeader\r\n\r\n"
            // Act & Assert
            shouldThrow<IllegalArgumentException> {
                HttpRequestParser.parse(makeStream(raw))
            }
        }
    }
})
