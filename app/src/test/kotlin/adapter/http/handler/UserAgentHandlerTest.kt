package adapter.http.handler

import adapter.http.HttpContext
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UserAgentHandlerTest : DescribeSpec({

    fun makeContext(headers: Map<String, String>) = HttpContext(
        request = HttpRequest(
            method = HttpMethod.GET,
            target = "/user-agent",
            protocol = HttpProtocol.HTTP11,
            headers = headers,
            body = byteArrayOf(),
        ),
    )

    describe("UserAgentHandler") {

        it("returns the User-Agent header value as the response body") {
            // Arrange
            val ctx = makeContext(mapOf("User-Agent" to "TestClient/1.0"))
            // Act
            val response = UserAgentHandler.get(ctx)
            // Assert
            response.status shouldBe HttpStatus.OK_200
            String(response.body!!, Charsets.UTF_8) shouldBe "TestClient/1.0"
        }

        it("returns 'Unknown' when the User-Agent header is absent") {
            // Arrange
            val ctx = makeContext(emptyMap())
            // Act
            val response = UserAgentHandler.get(ctx)
            // Assert
            String(response.body!!, Charsets.UTF_8) shouldBe "Unknown"
        }
    }
})
