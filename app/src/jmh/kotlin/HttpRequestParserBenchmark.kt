package benchmark

import infrastructure.http.HttpRequestParser
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

@State(Scope.Thread)
open class HttpRequestParserBenchmark {
    private lateinit var simpleGetBytes: ByteArray
    private lateinit var getWithHeadersBytes: ByteArray
    private lateinit var postWithBodyBytes: ByteArray

    @Setup(Level.Trial)
    fun setup() {
        simpleGetBytes = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".toByteArray()

        getWithHeadersBytes =
            buildString {
                append("GET /echo/hello HTTP/1.1\r\n")
                append("Host: localhost\r\n")
                append("User-Agent: JMH-Benchmark/1.0\r\n")
                append("Accept: text/plain\r\n")
                append("Accept-Encoding: gzip, deflate\r\n")
                append("Connection: keep-alive\r\n")
                append("\r\n")
            }.toByteArray()

        val body = """{"username":"benchmark","email":"bench@test.com"}"""
        postWithBodyBytes =
            buildString {
                append("POST /files/test.json HTTP/1.1\r\n")
                append("Host: localhost\r\n")
                append("Content-Type: application/json\r\n")
                append("Content-Length: ${body.length}\r\n")
                append("\r\n")
                append(body)
            }.toByteArray()
    }

    @Benchmark
    fun parseSimpleGet(): Any? {
        val stream = BufferedInputStream(ByteArrayInputStream(simpleGetBytes))
        return HttpRequestParser.parse(stream, 10_485_760)
    }

    @Benchmark
    fun parseGetWithHeaders(): Any? {
        val stream = BufferedInputStream(ByteArrayInputStream(getWithHeadersBytes))
        return HttpRequestParser.parse(stream, 10_485_760)
    }

    @Benchmark
    fun parsePostWithBody(): Any? {
        val stream = BufferedInputStream(ByteArrayInputStream(postWithBodyBytes))
        return HttpRequestParser.parse(stream, 10_485_760)
    }
}
