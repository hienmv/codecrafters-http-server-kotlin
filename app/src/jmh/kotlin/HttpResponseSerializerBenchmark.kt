package benchmark

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import domain.vo.HttpContentEncoding
import domain.vo.HttpContentType
import domain.vo.HttpProtocol
import infrastructure.http.HttpResponseSerializer
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Thread)
open class HttpResponseSerializerBenchmark {
    private lateinit var emptyResponse: HttpResponse
    private lateinit var textResponse: HttpResponse
    private lateinit var gzipResponse: HttpResponse
    private lateinit var largeBodyResponse: HttpResponse

    @Setup(Level.Trial)
    fun setup() {
        emptyResponse =
            HttpResponse(
                status = HttpStatus.OK_200,
                protocol = HttpProtocol.HTTP11,
            )

        textResponse =
            HttpResponse(
                status = HttpStatus.OK_200,
                protocol = HttpProtocol.HTTP11,
                headers =
                    mapOf(
                        "Content-Type" to HttpContentType.TEXT.value,
                    ),
                body = "Hello, World!".toByteArray(),
            )

        gzipResponse =
            HttpResponse(
                status = HttpStatus.OK_200,
                protocol = HttpProtocol.HTTP11,
                headers =
                    mapOf(
                        "Content-Type" to HttpContentType.TEXT.value,
                        "Content-Encoding" to HttpContentEncoding.GZIP.value,
                    ),
                body = "Hello, World! This is a response that will be gzip-compressed.".toByteArray(),
            )

        largeBodyResponse =
            HttpResponse(
                status = HttpStatus.OK_200,
                protocol = HttpProtocol.HTTP11,
                headers =
                    mapOf(
                        "Content-Type" to HttpContentType.OCTET_STREAM.value,
                    ),
                body = ByteArray(8192) { (it % 256).toByte() },
            )
    }

    @Benchmark
    fun serializeEmpty(): Any = HttpResponseSerializer.serialize(emptyResponse)

    @Benchmark
    fun serializeText(): Any = HttpResponseSerializer.serialize(textResponse)

    @Benchmark
    fun serializeGzip(): Any = HttpResponseSerializer.serialize(gzipResponse)

    @Benchmark
    fun serializeLargeBody(): Any = HttpResponseSerializer.serialize(largeBodyResponse)
}
