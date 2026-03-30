package benchmark

import adapter.http.HttpResponseFactory
import adapter.http.Router
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State

@State(Scope.Thread)
open class RouterBenchmark {
    private lateinit var router: Router
    private lateinit var rootRequest: HttpRequest
    private lateinit var echoRequest: HttpRequest
    private lateinit var fileRequest: HttpRequest
    private lateinit var notFoundRequest: HttpRequest

    @Setup(Level.Trial)
    fun setup() {
        router =
            Router().apply {
                get("/") { ctx -> ctx.result(HttpStatus.OK_200) }
                get("/echo/{text}") { ctx -> ctx.resultText(ctx.pathParam("text")) }
                get("/user-agent") { ctx -> ctx.resultText(ctx.request.headers["User-Agent"] ?: "") }
                get("/files/{fileName}") { ctx -> HttpResponseFactory.bytes(byteArrayOf(1, 2, 3)) }
                post("/files/{fileName}") { ctx -> HttpResponseFactory.empty(HttpStatus.CREATED_201) }
            }

        rootRequest = request(HttpMethod.GET, "/")
        echoRequest = request(HttpMethod.GET, "/echo/hello-world")
        fileRequest = request(HttpMethod.GET, "/files/test.txt")
        notFoundRequest = request(HttpMethod.GET, "/not/a/real/path")
    }

    private fun request(
        method: HttpMethod,
        target: String,
    ) = HttpRequest(
        method = method,
        target = target,
        protocol = HttpProtocol.HTTP11,
        headers = mapOf("User-Agent" to "JMH-Benchmark/1.0"),
        body = byteArrayOf(),
    )

    @Benchmark
    fun dispatchRoot(): Any = router.dispatch(rootRequest)

    @Benchmark
    fun dispatchEchoWithPathParam(): Any = router.dispatch(echoRequest)

    @Benchmark
    fun dispatchFileWithPathParam(): Any = router.dispatch(fileRequest)

    @Benchmark
    fun dispatchNotFound(): Any = router.dispatch(notFoundRequest)
}
