package fuzz

import com.code_intelligence.jazzer.junit.FuzzTest
import testutils.TestServerFactory
import java.net.Socket
import java.net.SocketException

class HttpServerFuzzTest {
    companion object {
        private val server = TestServerFactory.create().also { it.start() }
    }

    @FuzzTest(maxDuration = "60s")
    fun fuzzServerWithRandomBytes(data: ByteArray) {
        if (data.isEmpty()) return

        try {
            Socket("localhost", server.port).use { clientSocket ->
                clientSocket.soTimeout = 1000
                clientSocket.getOutputStream().write(data)
                clientSocket.getOutputStream().flush()

                // Read whatever the server sends back
                val buf = ByteArray(4096)
                try {
                    val n = clientSocket.getInputStream().read(buf)
                    if (n > 0) {
                        val response = String(buf, 0, n)
                        // Server should never return 5xx for malformed client input
                        if (response.startsWith("HTTP/")) {
                            val statusCode = response.substringAfter(" ").substringBefore(" ").toIntOrNull()
                            if (statusCode != null && statusCode in 500..599) {
                                throw AssertionError(
                                    "Server returned $statusCode for fuzzed input. " +
                                        "Input (${data.size} bytes): ${data.take(100).toList()}",
                                )
                            }
                        }
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // Server closed connection or timed out — acceptable for garbage input
                }
            }
        } catch (_: SocketException) {
            // Connection refused/reset — server may have closed it, which is fine
        } catch (_: java.io.IOException) {
            // Broken pipe, etc. — acceptable
        }
    }
}
