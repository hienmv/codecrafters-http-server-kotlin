package infrastructure.http.error

import domain.exception.PayloadTooLargeException
import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PayloadTooLargeRequestErrorHandlerTest :
    DescribeSpec({

        describe("PayloadTooLargeRequestErrorHandler") {

            it("returns 413 Payload Too Large status") {
                // Arrange
                val exception = PayloadTooLargeException(10 * 1024 * 1024)
                // Act
                val response = PayloadTooLargeRequestErrorHandler.handle(exception)
                // Assert
                response.status shouldBe HttpStatus.PAYLOAD_TOO_LARGE_413
            }

            it("includes the exception message in the response body") {
                // Arrange
                val exception = PayloadTooLargeException(10 * 1024 * 1024)
                // Act
                val response = PayloadTooLargeRequestErrorHandler.handle(exception)
                // Assert
                String(response.body!!, Charsets.UTF_8) shouldBe exception.message
            }
        }
    })
