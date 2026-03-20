package fuzz

import com.code_intelligence.jazzer.junit.FuzzTest
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import infrastructure.http.HttpResponseSerializer

class HttpResponseSerializerFuzzTest {
    @FuzzTest(maxDuration = "60s")
    fun fuzzSerializeWithRandomHeaders(data: ByteArray) {
        // Split the fuzz input into two parts to generate header key and value
        if (data.size < 2) return
        val splitPoint = data[0].toInt().and(0xFF) % data.size.coerceAtLeast(1)
        val headerKey = String(data.sliceArray(1..maxOf(1, splitPoint)), Charsets.ISO_8859_1)
        val headerValue = String(data.sliceArray(minOf(splitPoint + 1, data.size - 1) until data.size), Charsets.ISO_8859_1)

        val response =
            HttpResponse(
                status = HttpStatus.OK_200,
                headers = mapOf(headerKey to headerValue),
                body = "test".toByteArray(),
            )
        val serialized = String(HttpResponseSerializer.serialize(response), Charsets.UTF_8)

        // The serialized output must never contain an un-sanitised CRLF inside a header line.
        // Split by \r\n — each line before the blank separator should be a single header or the status line.
        val headerSection = serialized.substringBefore("\r\n\r\n")
        val lines = headerSection.split("\r\n")
        // First line is status line, remaining are headers.
        // No line should be empty (that would mean a bare CRLF leaked through).
        for (line in lines) {
            if (line.isEmpty()) {
                throw AssertionError("CRLF injection detected: empty line in header section. Full output:\n$serialized")
            }
        }
    }
}
