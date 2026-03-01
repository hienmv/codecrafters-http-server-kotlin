package infrastructure.http.enricher

import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

class GzipEncodingEnricherTest :
    DescribeSpec({

        fun makeRequest(headers: Map<String, String> = emptyMap()) =
            HttpRequest(
                method = HttpMethod.GET,
                target = "/",
                protocol = HttpProtocol.HTTP11,
                headers = headers,
                body = byteArrayOf(),
            )

        fun makeResponse(body: ByteArray? = "data".toByteArray()) = HttpResponse(status = HttpStatus.OK_200, body = body)

        describe("GzipEncodingEnricher") {

            it("adds Content-Encoding: gzip when Accept-Encoding contains gzip") {
                // Arrange
                val request = makeRequest(mapOf("Accept-Encoding" to "gzip"))
                val response = makeResponse()
                // Act
                val enriched = GzipEncodingEnricher.enrich(request, response)
                // Assert
                enriched.headers shouldContainKey "Content-Encoding"
                enriched.headers["Content-Encoding"] shouldBe "gzip"
            }

            it("does not modify the response when Accept-Encoding header is absent") {
                // Arrange
                val request = makeRequest()
                val response = makeResponse()
                // Act
                val enriched = GzipEncodingEnricher.enrich(request, response)
                // Assert
                enriched.headers shouldNotContainKey "Content-Encoding"
            }

            it("does not modify the response when Accept-Encoding does not include gzip") {
                // Arrange
                val request = makeRequest(mapOf("Accept-Encoding" to "identity"))
                val response = makeResponse()
                // Act
                val enriched = GzipEncodingEnricher.enrich(request, response)
                // Assert
                enriched.headers shouldNotContainKey "Content-Encoding"
            }

            it("adds gzip header when Accept-Encoding is a comma-separated list containing gzip") {
                // Arrange
                val request = makeRequest(mapOf("Accept-Encoding" to "deflate, gzip, br"))
                val response = makeResponse()
                // Act
                val enriched = GzipEncodingEnricher.enrich(request, response)
                // Assert
                enriched.headers["Content-Encoding"] shouldBe "gzip"
            }

            it("does not modify the response when the response body is null") {
                // Arrange
                val request = makeRequest(mapOf("Accept-Encoding" to "gzip"))
                val response = makeResponse(body = null)
                // Act
                val enriched = GzipEncodingEnricher.enrich(request, response)
                // Assert
                enriched.headers shouldNotContainKey "Content-Encoding"
            }
        }
    })
