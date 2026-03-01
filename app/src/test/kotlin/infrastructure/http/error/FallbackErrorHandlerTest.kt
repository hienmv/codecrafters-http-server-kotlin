package infrastructure.http.error

import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FallbackErrorHandlerTest :
    DescribeSpec({

        describe("FallbackErrorHandler") {

            it("returns 500 Internal Server Error for any throwable") {
                // Arrange
                val exception = RuntimeException("something broke")
                // Act
                val response = FallbackErrorHandler.handle(exception)
                // Assert
                response.status shouldBe HttpStatus.SERVER_ERROR_500
            }
        }
    })
