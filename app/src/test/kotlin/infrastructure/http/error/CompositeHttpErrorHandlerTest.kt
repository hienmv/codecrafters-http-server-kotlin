package infrastructure.http.error

import adapter.http.HttpResponseFactory
import adapter.http.port.HttpErrorHandler
import domain.exception.ResourceNotFoundException
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CompositeHttpErrorHandlerTest : DescribeSpec({

    describe("CompositeHttpErrorHandler") {

        it("routes ResourceNotFoundException to its mapped handler") {
            // Arrange
            val handler = CompositeHttpErrorHandler(
                handlers = listOf(
                    ResourceNotFoundException::class to NotFoundErrorHandler,
                    IllegalArgumentException::class to InvalidRequestErrorHandler,
                ),
                fallbackHandler = FallbackErrorHandler,
            )
            // Act
            val response = handler.handle(ResourceNotFoundException("file.txt"))
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }

        it("routes IllegalArgumentException to its mapped handler") {
            // Arrange
            val handler = CompositeHttpErrorHandler(
                handlers = listOf(
                    ResourceNotFoundException::class to NotFoundErrorHandler,
                    IllegalArgumentException::class to InvalidRequestErrorHandler,
                ),
                fallbackHandler = FallbackErrorHandler,
            )
            // Act
            val response = handler.handle(IllegalArgumentException("bad input"))
            // Assert
            response.status shouldBe HttpStatus.BAD_REQUEST_400
        }

        it("falls back to the fallback handler for an unmapped exception") {
            // Arrange
            val handler = CompositeHttpErrorHandler(
                handlers = emptyList(),
                fallbackHandler = FallbackErrorHandler,
            )
            // Act
            val response = handler.handle(RuntimeException("unexpected"))
            // Assert
            response.status shouldBe HttpStatus.SERVER_ERROR_500
        }

        it("uses the first matching handler when multiple handlers match the same type") {
            // Arrange
            val firstHandler = object : HttpErrorHandler {
                override fun handle(t: Throwable): HttpResponse =
                    HttpResponseFactory.error(HttpStatus.BAD_REQUEST_400, "first")
            }
            val secondHandler = object : HttpErrorHandler {
                override fun handle(t: Throwable): HttpResponse =
                    HttpResponseFactory.error(HttpStatus.SERVER_ERROR_500, "second")
            }
            val handler = CompositeHttpErrorHandler(
                handlers = listOf(
                    RuntimeException::class to firstHandler,
                    RuntimeException::class to secondHandler,
                ),
                fallbackHandler = FallbackErrorHandler,
            )
            // Act
            val response = handler.handle(RuntimeException("error"))
            // Assert
            String(response.body!!, Charsets.UTF_8) shouldBe "first"
        }
    }
})
