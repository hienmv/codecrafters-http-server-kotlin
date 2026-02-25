package adapter.http

import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class HttpResponseFactoryTest : DescribeSpec({

    describe("HttpResponseFactory") {

        describe("empty") {

            it("returns response with given status and no body") {
                // Arrange & Act
                val response = HttpResponseFactory.empty(HttpStatus.OK_200)
                // Assert
                response.status shouldBe HttpStatus.OK_200
                response.body.shouldBeNull()
            }
        }

        describe("text") {

            it("returns 200 with text/plain content-type header and UTF-8 encoded body") {
                // Arrange & Act
                val response = HttpResponseFactory.text("hello")
                // Assert
                response.status shouldBe HttpStatus.OK_200
                response.headers["Content-Type"] shouldBe "text/plain; charset=utf-8"
                String(response.body!!, Charsets.UTF_8) shouldBe "hello"
            }
        }

        describe("bytes") {

            it("returns 200 with octet-stream content-type and the given bytes as body") {
                // Arrange
                val content = byteArrayOf(1, 2, 3)
                // Act
                val response = HttpResponseFactory.bytes(content)
                // Assert
                response.status shouldBe HttpStatus.OK_200
                response.headers["Content-Type"] shouldBe "application/octet-stream"
                response.body!!.toList() shouldBe content.toList()
            }
        }

        describe("error") {

            it("uses the status message as body when no custom message is provided") {
                // Arrange & Act
                val response = HttpResponseFactory.error(HttpStatus.NOT_FOUND_404)
                // Assert
                response.status shouldBe HttpStatus.NOT_FOUND_404
                String(response.body!!, Charsets.UTF_8) shouldBe "Not Found"
            }

            it("uses a custom message as body when provided") {
                // Arrange & Act
                val response = HttpResponseFactory.error(HttpStatus.NOT_FOUND_404, "custom message")
                // Assert
                String(response.body!!, Charsets.UTF_8) shouldBe "custom message"
            }
        }
    }
})
