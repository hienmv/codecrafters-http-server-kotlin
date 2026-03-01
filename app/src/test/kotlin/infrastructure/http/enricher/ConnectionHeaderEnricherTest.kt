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

class ConnectionHeaderEnricherTest :
    DescribeSpec({

        fun makeRequest(headers: Map<String, String> = emptyMap()) =
            HttpRequest(
                method = HttpMethod.GET,
                target = "/",
                protocol = HttpProtocol.HTTP11,
                headers = headers,
                body = byteArrayOf(),
            )

        val baseResponse = HttpResponse(status = HttpStatus.OK_200)

        describe("ConnectionHeaderEnricher") {

            it("adds Connection: close to the response when request has Connection: close") {
                // Arrange
                val request = makeRequest(mapOf("Connection" to "close"))
                // Act
                val enriched = ConnectionHeaderEnricher.enrich(request, baseResponse)
                // Assert
                enriched.headers shouldContainKey "Connection"
                enriched.headers["Connection"] shouldBe "close"
            }

            it("does not add Connection header when request has no Connection header") {
                // Arrange
                val request = makeRequest()
                // Act
                val enriched = ConnectionHeaderEnricher.enrich(request, baseResponse)
                // Assert
                enriched.headers shouldNotContainKey "Connection"
            }

            it("matches Connection header value case-insensitively") {
                // Arrange
                val request = makeRequest(mapOf("Connection" to "CLOSE"))
                // Act
                val enriched = ConnectionHeaderEnricher.enrich(request, baseResponse)
                // Assert
                enriched.headers["Connection"] shouldBe "close"
            }
        }
    })
