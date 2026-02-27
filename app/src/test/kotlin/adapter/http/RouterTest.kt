package adapter.http

import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import infrastructure.http.enricher.GzipEncodingEnricher
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

class RouterTest : DescribeSpec({

    fun makeRequest(
        method: HttpMethod = HttpMethod.GET,
        target: String = "/",
        headers: Map<String, String> = emptyMap(),
    ) = HttpRequest(method = method, target = target, protocol = HttpProtocol.HTTP11, headers = headers, body = byteArrayOf())

    describe("Router") {

        it("returns 404 for unknown route") {
            // Arrange
            val router = Router()
            val request = makeRequest(target = "/unknown")
            // Act
            val response = router.dispatch(request)
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }

        it("routes GET request to registered path") {
            // Arrange
            val router = Router().get("/") { ctx -> ctx.result(HttpStatus.OK_200) }
            val request = makeRequest(target = "/")
            // Act
            val response = router.dispatch(request)
            // Assert
            response.status shouldBe HttpStatus.OK_200
        }

        it("returns 404 when HTTP method does not match registered route") {
            // Arrange
            val router = Router().get("/") { ctx -> ctx.result(HttpStatus.OK_200) }
            val request = makeRequest(method = HttpMethod.POST, target = "/")
            // Act
            val response = router.dispatch(request)
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }

        it("extracts single path parameter") {
            // Arrange
            var capturedParam = ""
            val router = Router().get("/echo/{text}") { ctx ->
                capturedParam = ctx.pathParam("text")
                ctx.result(HttpStatus.OK_200)
            }
            val request = makeRequest(target = "/echo/hello")
            // Act
            router.dispatch(request)
            // Assert
            capturedParam shouldBe "hello"
        }

        it("extracts multiple path parameters") {
            // Arrange
            var paramA = ""
            var paramB = ""
            val router = Router().get("/{a}/{b}") { ctx ->
                paramA = ctx.pathParam("a")
                paramB = ctx.pathParam("b")
                ctx.result(HttpStatus.OK_200)
            }
            val request = makeRequest(target = "/foo/bar")
            // Act
            router.dispatch(request)
            // Assert
            paramA shouldBe "foo"
            paramB shouldBe "bar"
        }

        it("returns 404 when path segment count does not match") {
            // Arrange
            val router = Router().get("/a/b") { ctx -> ctx.result(HttpStatus.OK_200) }
            val request = makeRequest(target = "/a")
            // Act
            val response = router.dispatch(request)
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }

        it("matches exact path without parameters") {
            // Arrange
            var handlerCalled = false
            val router = Router().get("/user-agent") { ctx ->
                handlerCalled = true
                ctx.result(HttpStatus.OK_200)
            }
            val request = makeRequest(target = "/user-agent")
            // Act
            router.dispatch(request)
            // Assert
            handlerCalled shouldBe true
        }

        it("applies enrichers to the dispatched response") {
            // Arrange
            val router = Router(enrichers = listOf(GzipEncodingEnricher))
                .get("/echo/{text}") { ctx -> ctx.resultText(ctx.pathParam("text")) }
            val request = makeRequest(target = "/echo/hello", headers = mapOf("Accept-Encoding" to "gzip"))
            // Act
            val response = router.dispatch(request)
            // Assert
            response.headers shouldContainKey "Content-Encoding"
            response.headers["Content-Encoding"] shouldBe "gzip"
        }

        it("URL-decodes path parameters") {
            // Arrange
            var capturedParam = ""
            val router = Router().get("/echo/{text}") { ctx ->
                capturedParam = ctx.pathParam("text")
                ctx.result(HttpStatus.OK_200)
            }
            val request = makeRequest(target = "/echo/hello%20world")
            // Act
            router.dispatch(request)
            // Assert
            capturedParam shouldBe "hello world"
        }

        it("POST route does not match a GET request") {
            // Arrange
            val router = Router().post("/files/{f}") { ctx -> ctx.result(HttpStatus.CREATED_201) }
            val request = makeRequest(method = HttpMethod.GET, target = "/files/test")
            // Act
            val response = router.dispatch(request)
            // Assert
            response.status shouldBe HttpStatus.NOT_FOUND_404
        }
    }
})
