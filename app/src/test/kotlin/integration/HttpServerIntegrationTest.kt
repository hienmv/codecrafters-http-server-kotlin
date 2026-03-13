package integration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import java.net.Socket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.io.path.createTempDirectory

class HttpServerIntegrationTest :
    DescribeSpec({
        val tmpDir = createTempDirectory("http-server-integration-test")
        val server = TestServerFactory.create(tmpDir.toString())
        val client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

        fun get(
            path: String,
            vararg headers: Pair<String, String>,
        ): HttpResponse<String> {
            val builder =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:${server.port}$path"))
                    .GET()
            headers.forEach { (k, v) -> builder.header(k, v) }
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        }

        fun post(
            path: String,
            body: String,
        ): HttpResponse<String> {
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("http://localhost:${server.port}$path"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()
            return client.send(request, HttpResponse.BodyHandlers.ofString())
        }

        beforeSpec { server.start() }
        afterSpec {
            server.stop()
            tmpDir.toFile().deleteRecursively()
        }

        describe("GET /") {
            it("returns 200 OK without body") {
                val response = get("/")
                response.statusCode() shouldBe 200
                response.body().shouldBeEmpty()
            }
        }

        describe("GET /echo/{text}") {
            it("returns 200 with the text as body") {
                val response = get("/echo/hello")
                response.statusCode() shouldBe 200
                response.body() shouldBe "hello"
            }

            it("returns text/plain content type") {
                val response = get("/echo/hello")
                response.headers().firstValue("content-type").get() shouldContain "text/plain"
            }

            it("handles URL-encoded text") {
                val response = get("/echo/hello%20world")
                response.statusCode() shouldBe 200
                response.body() shouldBe "hello world"
            }
        }

        describe("GET /user-agent") {
            it("echoes the User-Agent header value") {
                val response = get("/user-agent", "User-Agent" to "TestClient/1.0")
                response.statusCode() shouldBe 200
                response.body() shouldBe "TestClient/1.0"
            }
        }

        describe("GET /files/{name}") {
            it("returns 200 with file content when the file exists") {
                tmpDir.resolve("hello.txt").toFile().writeText("file content")
                val response = get("/files/hello.txt")
                response.statusCode() shouldBe 200
                response.body() shouldBe "file content"
            }

            it("returns 404 when the file does not exist") {
                val response = get("/files/nonexistent.txt")
                response.statusCode() shouldBe 404
            }
        }

        describe("POST /files/{name}") {
            it("creates the file and returns 201") {
                val response = post("/files/created.txt", "new content")
                response.statusCode() shouldBe 201
                tmpDir.resolve("created.txt").toFile().readText() shouldBe "new content"
            }
        }

        describe("error handling") {
            it("returns 404 for unknown routes") {
                val response = get("/nonexistent")
                response.statusCode() shouldBe 404
            }
        }

        describe("keep-alive") {
            it("reuses the same TCP connection for multiple requests") {
                Socket("localhost", server.port).use { clientSocket ->
                    val out = clientSocket.getOutputStream()
                    val input = clientSocket.getInputStream().bufferedReader()
                    repeat(2) {
                        // send raw HTTP request over the socket
                        out.write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray())
                        out.flush()
                        // read response until blank line (end of headers) — body is empty for GET /
                        val responseStatusLine = input.readLine()
                        responseStatusLine shouldContain "200"
                        while (input.readLine()?.isNotEmpty() == true) { /* drain headers */ }
                    }
                }
            }

            it("includes Connection: close when client requests it") {
                // HttpClient restricts the Connection header, so use a raw socket
                Socket("localhost", server.port).use { clientSocket ->
                    clientSocket.getOutputStream().write(
                        "GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray(),
                    )
                    clientSocket.getOutputStream().flush()
                    val rawResponse = clientSocket.getInputStream().bufferedReader().readText()
                    rawResponse shouldContain "Connection: close"
                }
            }
        }

        describe("gzip encoding") {
            it("returns gzip-compressed response when Accept-Encoding: gzip is sent") {
                val request =
                    HttpRequest
                        .newBuilder()
                        .uri(URI.create("http://localhost:${server.port}/echo/hello"))
                        .header("Accept-Encoding", "gzip")
                        .GET()
                        .build()
                // read raw bytes — HttpClient does not decompress gzip automatically
                val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                response.statusCode() shouldBe 200
                response.headers().firstValue("content-encoding").get() shouldBe "gzip"
            }
        }

        describe("POST then GET round trip") {
            it("GET returns the body that was POST-ed") {
                post("/files/roundtrip.txt", "round trip content")
                val response = get("/files/roundtrip.txt")
                response.statusCode() shouldBe 200
                response.body() shouldBe "round trip content"
            }
        }

        describe("path traversal") {
            it("returns 404 for path traversal attempts") {
                Socket("localhost", server.port).use { clientSocket ->
                    // do not directly use the HttpClient for this test
                    // since it may normalize the path and prevent testing the raw path traversal attack
                    clientSocket.getOutputStream().write(
                        "GET /files/../../../etc/passwd HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray(),
                    )
                    clientSocket.getOutputStream().flush()
                    val statusLine = clientSocket.getInputStream().bufferedReader().readLine()
                    statusLine shouldContain "404"
                }
            }
        }
    })
