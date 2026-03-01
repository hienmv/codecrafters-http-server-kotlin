package adapter.http

import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HttpContextTest :
    DescribeSpec({

        fun makeRequest(headers: Map<String, String> = emptyMap()) =
            HttpRequest(
                method = HttpMethod.GET,
                target = "/",
                protocol = HttpProtocol.HTTP11,
                headers = headers,
                body = byteArrayOf(),
            )

        describe("HttpContext") {

            describe("pathParam") {

                it("returns the value for an existing path parameter") {
                    // Arrange
                    val ctx = HttpContext(makeRequest(), pathParams = mapOf("name" to "alice"))
                    // Act
                    val result = ctx.pathParam("name")
                    // Assert
                    result shouldBe "alice"
                }

                it("returns empty string for a missing path parameter") {
                    // Arrange
                    val ctx = HttpContext(makeRequest(), pathParams = emptyMap())
                    // Act
                    val result = ctx.pathParam("missing")
                    // Assert
                    result shouldBe ""
                }
            }

            describe("result") {

                it("returns an empty response with the given status") {
                    // Arrange
                    val ctx = HttpContext(makeRequest())
                    // Act
                    val response = ctx.result(HttpStatus.OK_200)
                    // Assert
                    response.status shouldBe HttpStatus.OK_200
                }
            }

            describe("resultText") {

                it("returns a text response with the correct body") {
                    // Arrange
                    val ctx = HttpContext(makeRequest())
                    // Act
                    val response = ctx.resultText("hi")
                    // Assert
                    String(response.body!!, Charsets.UTF_8) shouldBe "hi"
                }
            }

            describe("resultBytes") {

                it("returns a bytes response with the correct body") {
                    // Arrange
                    val ctx = HttpContext(makeRequest())
                    val bytes = byteArrayOf(10, 20, 30)
                    // Act
                    val response = ctx.resultBytes(bytes)
                    // Assert
                    response.body!!.toList() shouldBe bytes.toList()
                }
            }
        }
    })
