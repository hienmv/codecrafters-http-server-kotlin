package security

import adapter.http.HttpRequestAdapter
import adapter.http.Router
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import infrastructure.http.HttpConfig
import infrastructure.http.HttpServer
import infrastructure.http.error.CompositeHttpErrorHandler
import infrastructure.http.error.FallbackErrorHandler
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.net.Socket

class CrlfInjectionTest :
    DescribeSpec({
        // Custom handler that puts the path param into a response header value.
        // This simulates a handler that reflects user input into headers.
        val router =
            Router()
                .get("/header-echo/{value}") { ctx ->
                    HttpResponse(
                        status = HttpStatus.OK_200,
                        headers = mapOf("X-Echo" to ctx.pathParam("value")),
                    )
                }

        val server =
            HttpServer(
                config = HttpConfig(port = 0),
                adapter = HttpRequestAdapter(router),
                errorHandler = CompositeHttpErrorHandler(handlers = listOf(), fallbackHandler = FallbackErrorHandler),
            )

        beforeSpec { server.start() }
        afterSpec { server.stop() }

        fun rawGet(path: String): String {
            Socket("localhost", server.port).use { s ->
                s.getOutputStream().write(
                    "GET $path HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray(),
                )
                s.getOutputStream().flush()
                return s.getInputStream().bufferedReader().readText()
            }
        }

        describe("CRLF injection prevention") {
            it("strips CRLF from header value to prevent response splitting") {
                val rawResponse = rawGet("/header-echo/legitimate%0d%0aSet-Cookie:%20injected")
                // The CRLF should be stripped, concatenating the value onto one header line
                rawResponse shouldContain "X-Echo: legitimateSet-Cookie: injected\r\n"
                // "Set-Cookie" must NOT appear as a separate header line (preceded by \r\n)
                rawResponse shouldNotContain "\r\nSet-Cookie:"
            }

            it("strips CR alone from header value") {
                val rawResponse = rawGet("/header-echo/value%0dwith%0dcr")
                rawResponse shouldContain "X-Echo: valuewithcr\r\n"
            }

            it("strips LF alone from header value") {
                val rawResponse = rawGet("/header-echo/value%0awith%0alf")
                rawResponse shouldContain "X-Echo: valuewithlf\r\n"
            }
        }
    })
