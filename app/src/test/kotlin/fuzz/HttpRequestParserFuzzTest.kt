package fuzz

import com.code_intelligence.jazzer.junit.FuzzTest
import domain.exception.PayloadTooLargeException
import infrastructure.http.HttpRequestParser
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream

class HttpRequestParserFuzzTest {
    @FuzzTest(maxDuration = "60s")
    fun fuzzParse(data: ByteArray) {
        val stream = BufferedInputStream(ByteArrayInputStream(data))
        try {
            HttpRequestParser.parse(stream, maxRequestBodyBytes = 1024)
        } catch (_: IllegalArgumentException) {
            // Expected — malformed request line, unknown method/protocol, bad headers, etc.
        } catch (_: PayloadTooLargeException) {
            // Expected — Content-Length exceeds limit
        } catch (_: NumberFormatException) {
            // Expected — non-numeric Content-Length
        }
        // Any other exception (OutOfMemoryError, StackOverflowError, etc.) = bug
    }
}
