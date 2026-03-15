package security

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import testutils.TestServerFactory
import java.net.Socket
import java.net.SocketException

class SlowlorisTest :
    DescribeSpec({
        // TestServerFactory sets readTimeoutMs = 200ms for all test servers
        val server = TestServerFactory.create()

        beforeSpec { server.start() }
        afterSpec { server.stop() }

        describe("slowloris protection") {
            it("closes the connection when the client sends an incomplete request") {
                Socket("localhost", server.port).use { socket ->
                    // Send partial headers — no terminating blank line
                    socket.getOutputStream().write("GET / HTTP/1.1\r\nHost: localhost\r\n".toByteArray())
                    socket.getOutputStream().flush()

                    // Wait for the server's read timeout to expire
                    Thread.sleep(500)

                    // Server should have closed the connection
                    val result = socket.getInputStream().read()
                    result shouldBe -1
                }
            }

            it("closes idle keep-alive connections after timeout") {
                Socket("localhost", server.port).use { socket ->
                    val out = socket.getOutputStream()
                    val input = socket.getInputStream()

                    // First request completes successfully
                    out.write("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray())
                    out.flush()

                    // Read the full response
                    val buf = ByteArray(4096)
                    val n = input.read(buf)
                    val response = String(buf, 0, n)
                    response shouldContain "200"

                    // Now idle — don't send a second request. Wait for timeout.
                    Thread.sleep(500)

                    // Server should have closed the connection
                    val eof =
                        try {
                            input.read()
                        } catch (_: SocketException) {
                            -1
                        }
                    eof shouldBe -1
                }
            }

            it("still serves normal requests within the timeout") {
                Socket("localhost", server.port).use { socket ->
                    socket.getOutputStream().write(
                        "GET / HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray(),
                    )
                    socket.getOutputStream().flush()

                    val response = socket.getInputStream().bufferedReader().readText()
                    response shouldContain "200"
                }
            }
        }
    })
