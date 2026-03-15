package security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import testutils.TestServerFactory
import java.net.Socket
import kotlin.io.path.createTempDirectory

class PathTraversalTest :
    DescribeSpec({
        val tmpDir = createTempDirectory("http-server-integration-test")
        val server = TestServerFactory.create(tmpDir.toString())
        val secretFile = tmpDir.parent.resolve("secret.txt").toFile()
        beforeSpec {
            server.start()
            secretFile.writeText("secret content")
        }
        afterSpec {
            server.stop()
            tmpDir.toFile().deleteRecursively()
            secretFile.delete()
        }

        fun rawGet(path: String): String {
            Socket("localhost", server.port).use { s ->
                s.getOutputStream().write("GET $path HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray())
                s.getOutputStream().flush()
                return s.getInputStream().bufferedReader().readLine()
            }
        }

        describe("GET path traversal") {
            it("returns 404 for plain ../") {
                val statusLine = rawGet("/files/../secret.txt")
                statusLine shouldContain "404"
            }
            it("returns 404 for URL-encoded dots") {
                val statusLine = rawGet("/files/%2e%2e%2fsecret.txt")
                statusLine shouldContain "404"
            }
            it("returns 404 for URL-encoded slashes") {
                val statusLine = rawGet("/files/..%2fsecret.txt")
                statusLine shouldContain "404"
            }
        }

        describe("POST path traversal") {
            it("returns 404 for path traversal attempts") {
                Socket("localhost", server.port).use { clientSocket ->
                    // do not directly use the HttpClient for this test
                    // since it may normalize the path and prevent testing the raw path traversal attack
                    clientSocket.getOutputStream().write(
                        "POST /files/..%2fsecret.txt HTTP/1.1\r\nHost: localhost\r\nContent-Length: 3\r\n\r\nabc".toByteArray(),
                    )
                    clientSocket.getOutputStream().flush()
                    val statusLine = clientSocket.getInputStream().bufferedReader().readLine()
                    statusLine shouldContain "404"
                    secretFile.readText() shouldBe "secret content"
                }
            }
        }
    })
