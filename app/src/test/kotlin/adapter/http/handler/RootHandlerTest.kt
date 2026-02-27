package adapter.http.handler

import adapter.http.HttpContext
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class RootHandlerTest : DescribeSpec({

    describe("RootHandler") {

        it("returns 200 OK with no body") {
            // Arrange
            val request = HttpRequest(
                method = HttpMethod.GET,
                target = "/",
                protocol = HttpProtocol.HTTP11,
                body = byteArrayOf(),
            )
            val ctx = HttpContext(request)
            // Act
            val response = RootHandler.get(ctx)
            // Assert
            response.status shouldBe HttpStatus.OK_200
            response.body.shouldBeNull()
        }
    }
})
