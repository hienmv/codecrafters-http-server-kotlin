package infrastructure.http.error

import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class InvalidRequestErrorHandlerTest :
    DescribeSpec({

        describe("InvalidRequestErrorHandler") {

            it("returns 400 Bad Request with the exception message as the body") {
                // Arrange
                val exception = IllegalArgumentException("invalid header format")
                // Act
                val response = InvalidRequestErrorHandler.handle(exception)
                // Assert
                response.status shouldBe HttpStatus.BAD_REQUEST_400
                String(response.body!!, Charsets.UTF_8) shouldBe "invalid header format"
            }
        }
    })
