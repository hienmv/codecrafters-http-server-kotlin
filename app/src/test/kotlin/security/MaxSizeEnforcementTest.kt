package security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import testutils.TestServerFactory
import java.net.Socket
import kotlin.io.path.createTempDirectory

class MaxSizeEnforcementTest :
    DescribeSpec({
        // Use a small limit so tests run fast without allocating huge buffers
        val maxBytes = 1024
        val tmpDir = createTempDirectory("http-server-max-size-test")
        val server = TestServerFactory.create(tmpDir.toString(), maxRequestBodyBytes = maxBytes)

        beforeSpec { server.start() }
        afterSpec {
            server.stop()
            tmpDir.toFile().deleteRecursively()
        }

        fun rawPost(
            fileName: String,
            contentLength: Int,
            body: String? = null,
        ): String {
            Socket("localhost", server.port).use { s ->
                val request =
                    "POST /files/$fileName HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: $contentLength\r\n" +
                        "Connection: close\r\n" +
                        "\r\n" +
                        (body ?: "")
                s.getOutputStream().write(request.toByteArray())
                s.getOutputStream().flush()
                return s.getInputStream().bufferedReader().readLine()
            }
        }

        describe("POST with invalid content length") {
            it("returns 400 for negative content length") {
                val statusLine = rawPost("negative.txt", contentLength = -1)
                statusLine shouldContain "400"
            }
        }

        describe("POST max request body size enforcement") {
            it("returns 413 when content length exceeds the limit") {
                val statusLine = rawPost("oversized.txt", contentLength = maxBytes + 1)
                statusLine shouldContain "413"
                tmpDir.resolve("oversized.txt").toFile().exists() shouldBe false
            }

            it("returns 201 when content length is exactly at the limit") {
                val body = "a".repeat(maxBytes)
                val statusLine = rawPost("exact.txt", contentLength = maxBytes, body = body)
                statusLine shouldContain "201"
                tmpDir.resolve("exact.txt").toFile().readText() shouldBe body
            }

            it("returns 201 when content length is within the limit") {
                val body = "hello"
                val statusLine = rawPost("small.txt", contentLength = body.length, body = body)
                statusLine shouldContain "201"
                tmpDir.resolve("small.txt").toFile().readText() shouldBe "hello"
            }
        }
    })
