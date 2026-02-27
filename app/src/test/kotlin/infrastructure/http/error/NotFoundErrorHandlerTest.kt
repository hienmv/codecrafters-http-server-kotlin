package infrastructure.http.error

import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NotFoundErrorHandlerTest : DescribeSpec({

    describe("NotFoundErrorHandler") {

        it("returns a 404 Not Found response") {
            // Arrange
            val exception = RuntimeException("not found")
            // Act
            val response = NotFoundErrorHandler.handle(exception)
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }
    }
})
