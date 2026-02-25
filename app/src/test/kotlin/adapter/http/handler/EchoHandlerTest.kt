package adapter.http.handler

import adapter.http.HttpContext
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class EchoHandlerTest : DescribeSpec({

    fun makeContext(pathParams: Map<String, String> = emptyMap()) = HttpContext(
        request = HttpRequest(
            method = HttpMethod.GET,
            target = "/echo/hello",
            protocol = HttpProtocol.HTTP11,
            body = byteArrayOf(),
        ),
        pathParams = pathParams,
    )

    describe("EchoHandler") {

        it("echoes the text path parameter as a text/plain response") {
            // Arrange
            val ctx = makeContext(mapOf("text" to "hello"))
            // Act
            val response = EchoHandler.get(ctx)
            // Assert
            response.status shouldBe HttpStatus.OK_200
            response.headers["Content-Type"] shouldBe "text/plain; charset=utf-8"
            String(response.body!!, Charsets.UTF_8) shouldBe "hello"
        }

        it("echoes empty string when the text param is absent") {
            // Arrange
            val ctx = makeContext(emptyMap())
            // Act
            val response = EchoHandler.get(ctx)
            // Assert
            String(response.body!!, Charsets.UTF_8) shouldBe ""
        }
    }
})
