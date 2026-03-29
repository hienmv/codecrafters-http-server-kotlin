# HTTP Server — Kotlin

[![progress-banner](https://backend.codecrafters.io/progress/http-server/0ebddf4f-0b34-4aca-85f1-4f8e1d9c1f38)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

A from-scratch HTTP/1.1 server — started from [CodeCrafters](https://app.codecrafters.io/courses/http-server/overview), evolved into a high-performance design study.

---

## Running

```sh
# build once (re-run after code changes)
./gradlew installDist

# run the server
./run.sh                              # default directory: .
./run.sh --directory /path/to/files   # serve files from a custom directory
```

Listens on port `4221`.

```sh
# run functional tests (requires: pip install requests)
python3 scripts/test.py

# run load tests (requires: brew install wrk)
./scripts/load-test-wrk.sh

# run load tests with concurrency sweep (requires: brew install hey)
./scripts/load-test-hey.sh

# CPU/allocation/wall-clock flame graph (requires: brew install async-profiler wrk)
./scripts/async-profiler.sh                        # CPU (default)
PROFILE_EVENT=alloc ./scripts/async-profiler.sh    # allocation
PROFILE_EVENT=wall  ./scripts/async-profiler.sh    # wall-clock

# run fuzz tests (Jazzer, 60s per test)
./gradlew test --tests "fuzz.*"
```

---

## Supported Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | 200 OK |
| `GET` | `/echo/{text}` | Echoes `{text}` as plain text |
| `GET` | `/user-agent` | Returns the `User-Agent` request header |
| `GET` | `/files/{name}` | Serves a file from the configured directory |
| `POST` | `/files/{name}` | Writes request body to a file in the configured directory |

**HTTP/1.1 features supported:**
- Persistent connections (keep-alive)
- `Connection: close` graceful shutdown
- `Accept-Encoding: gzip` content negotiation and compression
- Binary-safe request/response bodies (`ByteArray`, not `String`)
- Path traversal protection on file endpoints
- Path parameter extraction (`{param}` in route patterns)
- Request body size limit (10 MB default, `413 Payload Too Large`)
- Read timeout / slowloris protection (30s default)
- CRLF injection prevention in response headers

---

## Architecture

Designed with **Hexagonal Architecture** (Ports & Adapters) and **DDD** principles. Dependencies point strictly inward — inner layers know nothing about outer layers.

```
┌─────────────────────────────────────────────────────────────┐
│                         App.kt                              │  Composition root
└──────────────────────────────┬──────────────────────────────┘
                               │ wires
          ┌────────────────────┼───────────────────┐
          ▼                    ▼                   ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│   adapters/http/ │  │ infrastructure/  │  │  infrastructure/     │
│  Router          │  │     http/        │  │    fileSystem/        │
│  HttpRequestAdap.│  │                  │  │  LocalFileRepository  │
│  HttpContext     │  │                  │  │                       │
│  handler/*       │  │                  │  │                       │
└────────┬─────────┘  └──────────────────┘  └──────────────────────┘
         │ depends on
         ▼
┌─────────────────────────────────────────────────────────────┐
│                       application/                          │
│   port/FileRepository     usecase/GetFileContent            │
│                           usecase/WriteFileContent          │
└──────────────────────────────┬──────────────────────────────┘
                               │ depends on
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                         domain/                             │
│   HttpRequest   HttpResponse   HttpStatus   HttpMethod      │
│   HttpProtocol  HttpContentType  HttpContentEncoding        │
└─────────────────────────────────────────────────────────────┘
```

### Layer responsibilities

**`domain/`** — Pure data. Value objects and entities. No dependencies, no I/O.

**`application/`** — Use cases and outbound ports. Use cases (`GetFileContent`, `WriteFileContent`) orchestrate domain logic with no HTTP awareness. `FileRepository` is a port interface — no `java.io`.

**`adapters/http/`** — Primary adapter. `Router` matches incoming HTTP requests to handlers via path parameter patterns (`{param}`). `HttpContext` is a stateless response builder. Handlers translate HTTP ↔ use case — they are the API definition layer.

**`infrastructure/http/`** — TCP and HTTP wire protocol. Parsing bytes into `HttpRequest`, serializing `HttpResponse` into bytes (with GZIP), managing TCP connection lifecycle and keep-alive.

**`infrastructure/fileSystem/`** — Local disk. Implements `FileRepository`. Owns path traversal protection and `java.io.File`.

**`App.kt`** — Composition root. The only place that knows all layers exist and wires them together. Exposes `buildServer(httpConfig, directoryPath)` — a top-level function shared by `main()` and the integration test harness. `main()` calls `buildServer()`, `server.start()`, then parks the main thread with `Thread.currentThread().join()`.

### Key design decisions

- **Handlers are API definitions** — live in `adapters/http/handler/`, not application. They translate HTTP → use case input → HTTP response. Use cases remain HTTP-agnostic and reusable by any adapter (CLI, gRPC).
- **`Router` owns path parameter extraction** — `matchPath()` splits pattern and path by `/`, matches `{param}` segments, injects extracted values into `HttpContext`.
- **`HttpContext` carries path params** — `ctx.pathParam("name")` gives handlers clean access without string manipulation.
- **`FileRepository` port** — the application layer never imports `java.io`. Swappable: local disk today, S3 or database tomorrow.
- **`HttpContentEncoding` in domain** — encoding names (`gzip`) are domain value objects shared between adapters (negotiation) and infrastructure (compression) without either depending on the other.

---

## Project Structure

```
app/src/main/kotlin/
├── domain/
│   ├── httpRequest/        HttpRequest, HttpMethod
│   ├── httpResponse/       HttpResponse, HttpStatus
│   ├── vo/                 HttpProtocol, HttpContentType, HttpContentEncoding
│   └── exception/          ResourceNotFoundException, PayloadTooLargeException
├── application/
│   ├── port/
│   │   └── FileRepository.kt        outbound port — no java.io
│   └── usecase/
│       ├── GetFileContent.kt         (fileName): ByteArray?
│       └── WriteFileContent.kt       (fileName, content): Unit
├── adapters/
│   └── http/
│       ├── handler/                  API definitions — translate HTTP ↔ use case
│       │   ├── RootHandler.kt
│       │   ├── EchoHandler.kt
│       │   ├── UserAgentHandler.kt
│       │   ├── GetFileContentHandler.kt
│       │   └── WriteFileContentHandler.kt
│       ├── port/
│       │   ├── HttpAdapter.kt            inbound port interface
│       │   ├── HttpErrorHandler.kt       error handler port
│       │   ├── HttpRequestContext.kt
│       │   ├── HttpResponseBuilder.kt
│       │   ├── HttpResponseEnricher.kt   cross-cutting response enrichment
│       │   └── ResponseWriter.kt         output port interface
│       ├── Router.kt                 path param routing, get/post registration
│       ├── HttpContext.kt            stateless response builder + pathParam()
│       ├── HttpRequestAdapter.kt     bridges infrastructure → router
│       └── HttpResponseFactory.kt    builds HttpResponse from status + body
├── infrastructure/
│   ├── http/
│   │   ├── enricher/
│   │   │   ├── GzipEncodingEnricher.kt   gzip compression enricher
│   │   │   └── ConnectionHeaderEnricher.kt
│   │   ├── error/
│   │   │   ├── CompositeHttpErrorHandler.kt
│   │   │   ├── FallbackErrorHandler.kt
│   │   │   ├── InvalidRequestErrorHandler.kt
│   │   │   ├── NotFoundErrorHandler.kt
│   │   │   └── PayloadTooLargeRequestErrorHandler.kt
│   │   ├── HttpConfig.kt             port, maxConcurrentConnections, maxRequestBodyBytes, readTimeoutMs
│   │   ├── HttpConnectionHandler.kt  connection lifecycle, keep-alive
│   │   ├── HttpRequestParser.kt      bytes → HttpRequest, enforces max body size
│   │   ├── HttpResponseSerializer.kt HttpResponse → bytes, GZIP, CRLF sanitisation
│   │   ├── HttpServer.kt             ServerSocket, accept loop, daemon threads, start/stop
│   │   └── SocketResponseWriter.kt   implements ResponseWriter
│   └── fileSystem/
│       └── LocalFileRepository.kt    implements FileRepository, path traversal protection
└── App.kt                            composition root, exposes buildServer()
```

---

## Roadmap

> Sections are ordered by priority. Items within each section are ordered by priority: bugs and security fixes first, then prerequisites before their dependents, then highest-impact / lowest-effort first.
>
> TDD workflow: set up the integration test harness (section 2) before starting work on any subsequent section. For each new feature, write the integration test first — red → green → refactor.

### 1. Correctness & Robustness

Fix active bugs before adding anything new.

- [x] **Malformed request line → 400** *(bug)* — `HttpRequestParser` now validates request line has 3 parts and header lines contain `:`, throwing `IllegalArgumentException` → 400. Unknown method and unsupported protocol also map to 400.
- [x] **`IOException` on write** *(bug)* — `SocketResponseWriter.writeResponse()` now catches `IOException`, logs it, and closes the output stream. Client disconnects no longer propagate to `HttpConnectionHandler`'s catch block.
- [x] **`Content-Length` truncation detection** *(bug)* — if the client disconnects before sending all declared bytes, the partial body is silently accepted. Detect `offset < contentLength` after the read loop and respond `400`.
- [x] **`data class` + `ByteArray` equality** *(bug)* — `ByteArray` fields in `data class` use reference equality, breaking `equals()`/`hashCode()`. Override both or switch to a regular `class`.
- [x] **Response header CRLF injection** — `HttpResponseSerializer` strips `\r` and `\n` from all header keys and values before writing to the wire. Prevents response splitting (RFC 7230).
- [x] **Request header CRLF injection** — not required: `HttpRequestParser` uses `\r\n` as the line terminator, so parsed header values structurally cannot contain `\r\n`.
- [x] **Max request size** — `HttpRequestParser` checks `Content-Length` against `MAX_REQUEST_BODY_BYTES` (10 MB) before allocating the body buffer. Returns `413 Payload Too Large` via `PayloadTooLargeException` → `PayloadTooLargeRequestErrorHandler`. Negative `Content-Length` rejected with `400`.
- [x] **Max concurrent connections** *(interim)* — `Semaphore(MAX_CONCURRENT_CONNECTIONS)` + `tryAcquire(timeout)` in `App.kt`. At capacity, sends `503 Service Unavailable` and closes. Will be superseded by `ThreadPoolExecutor` + `LinkedBlockingQueue` in `HttpServer` (section 5), which adds thread reuse and a non-blocking accept loop.

### 2. Testing

> **TDD:** set up the JVM integration test harness first — before implementing features in sections 3–7. For each feature, write the integration test (red), implement (green), then refactor. Unit tests (Kotest) are written alongside each implementation.

**Correctness**
- [x] **Unit tests (Kotest)** — `DescribeSpec` + AAA style, all layers. No mock library: ports faked with anonymous objects.
- [x] **Functional test suite** (`test.py`) — all routes, keep-alive, gzip, `Connection: close`, 20 concurrent requests.
- [x] **Integration tests (JVM)** — start server in a test process on a random port, send real HTTP requests via `java.net.http.HttpClient`. Covers full request-response cycle end-to-end. `TestServerFactory` uses `buildServer(httpConfig = HttpConfig(port = 0))` so the OS assigns a free port; `server.port` exposes it after `start()`.
- [x] **Security tests** — path traversal, CRLF injection, slowloris, max-size enforcement.
- [x] **Fuzz testing** — Jazzer-based fuzz tests (`@FuzzTest`) for `HttpRequestParser`, `HttpResponseSerializer`, and live `HttpServer`. Verifies no crashes on malformed input, no 5xx on garbage requests, and no CRLF injection in serialized responses.

**Performance benchmarks** *(run before and after each Performance step to measure the gain)*
- [x] **Load testing: `wrk`** — sustained RPS + latency percentiles (p50/p99/p999) against a running server. `scripts/load-test-wrk.sh` wraps `wrk -t4 -c400 -d30s` against `/`, `/echo/hello`, and `/user-agent`. First tool to reach for; establishes the baseline before each optimisation step.
- [x] **Load testing: `hey`** — concurrency sweep (10 → 100 → 1000 connections) to find the throughput cliff. `scripts/load-test-hey.sh` runs `hey -n 100000` at each concurrency level against `/`, `/echo/hello`, and `/user-agent`, then prints a summary table.
- [x] **Profiling: async-profiler** — flame graph of CPU and allocation on the hot path under load. Identifies which allocations to pool (Step 4) and which code path to NIO-ify (Step 3). `scripts/async-profiler.sh` wraps `asprof start/stop` around a `wrk` load run, producing an HTML flame graph in `build/profile-results/`. Configurable via `PROFILE_EVENT` (`cpu`, `alloc`, `wall`).
- [ ] **Micro-benchmarks (JMH)** — component-level throughput: `Router.dispatch()`, `HttpRequestParser.parse()`, `HttpResponseSerializer.serialize()`. Isolates hot-path regressions. Run after async-profiler identifies the hot component.
- [ ] **GC log analysis** — `-Xlog:gc*` to measure pause frequency and duration at each concurrency step. Validates whether ZGC/Shenandoah (Step 5) is necessary. Only relevant above ~500K RPS.

### 3. HTTP/1.1 Protocol
- [x] **Persistent connections (keep-alive)** — connection loop in `HttpConnectionHandler` reuses TCP connection across multiple requests.
- [x] **`Connection: close` graceful shutdown** — server detects header, adds `Connection: close` to response via enricher, closes after reply.
- [x] **`Accept-Encoding: gzip` negotiation** — `GzipEncodingEnricher` checks accepted encodings; `HttpResponseSerializer` compresses body with `GZIPOutputStream`.
- [x] **Binary-safe bodies** — `HttpRequest.body` and `HttpResponse.body` are `ByteArray` throughout. No UTF-8 corruption of binary files.
- [x] **Path traversal protection** — `LocalFileRepository` resolves canonical paths and rejects any path escaping the base directory.
- [ ] **Expand `HttpStatus`** — add `100 Continue`, `204 No Content`, `206 Partial Content`, `301/302 Redirect`, `304 Not Modified`, `405`, `408`, `415`, `429`, `501`, `505`. (`413` already added.) Prerequisite for method/protocol validation, `100 Continue` support, and range requests.
- [ ] **Multiple header values** — `HttpRequestParser` uses `associate` which silently drops duplicate header keys. Change to `Map<String, List<String>>` or comma-join per RFC 7230. Affects auth (`Authorization`) and content negotiation.
- [ ] **Method validation → 405** — unknown `HttpMethod` throws `IllegalArgumentException` mapping to 400. Correct response is `405 Method Not Allowed` with `Allow` header. Depends on expanded `HttpStatus`.
- [ ] **Protocol validation → 505** — unknown protocol string now maps to 400 (via `IllegalArgumentException`). Correct response is `505 HTTP Version Not Supported`. Depends on expanded `HttpStatus`.
- [ ] **HEAD method** — respond with headers only, no body. Simple; required by RFC 7231.
- [ ] **OPTIONS method** — return `Allow` header listing registered methods for the path. Prerequisite for CORS preflight.
- [ ] **CORS** — `Access-Control-Allow-*` response headers and preflight handling, configurable origin policy. Depends on OPTIONS.
- [ ] **`100 Continue`** — respond to `Expect: 100-continue` before reading large request bodies.
- [ ] **Range requests** — `Range: bytes=0-1023` for partial file downloads (video streaming, resumable downloads). Returns `206 Partial Content`. Depends on expanded `HttpStatus`.
- [ ] **Conditional requests: ETag** — `ETag` / `If-None-Match` / `If-Match` for hash-based cache validation. Returns `304 Not Modified` or `412 Precondition Failed`. Depends on expanded `HttpStatus`.
- [ ] **Conditional requests: timestamp** — `Last-Modified` / `If-Modified-Since` / `If-Unmodified-Since` for time-based cache validation.
- [ ] **HTTP/1.1 pipelining** — handle multiple in-flight requests on the same connection without waiting for each response.
- [ ] **HTTP/2** — multiplexing, header compression (HPACK), server push. Requires TLS + ALPN.

### 4. Routing
- [x] **Registration-based router** — `Router.get(path, handler)` / `Router.post(path, handler)` replaces hardcoded `when` block.
- [x] **Path parameter extraction** — `{param}` in route patterns. `Router.matchPath()` splits by `/`, extracts named segments into `HttpContext`, accessed via `ctx.pathParam("name")`.
- [ ] **Query string parsing** — `target` is stored raw (e.g. `/path?foo=bar`). Parse into `Map<String, String>` and expose via `ctx.queryParam("name")`. `Router.matchPath()` must strip query string before path matching.

### 5. Architecture
- [x] **Hexagonal / Clean Architecture** — strict layer separation: domain → application → adapters → infrastructure. Dependencies point inward only.
- [x] **DDD** — use cases (`GetFileContent`, `WriteFileContent`) in application layer. Handlers are HTTP-specific adapters, not use cases.
- [x] **`FileRepository` port (DIP)** — application layer defines the interface; infrastructure implements it. No `java.io` in application or domain.
- [x] **Handlers as API definitions** — live in `adapters/http/handler/`, translate HTTP ↔ use case. Use cases remain HTTP-agnostic.
- [x] **`HttpContext` stateless builder** — each `result*` method creates a fresh headers map. No shared mutable state between calls. Carries `pathParams`.
- [x] **`SO_REUSEADDR` before bind** — `ServerSocket()` created unbound, `reuseAddress = true` set, then `bind()` called.
- [x] **Response enrichers** — `HttpResponseEnricher` port + `GzipEncodingEnricher` + `ConnectionHeaderEnricher`. Applies cross-cutting response headers after handler returns; wired into `Router`.
- [x] **`HttpServer` class** — `ServerSocket`, bind, accept loop, and thread management live in `infrastructure/http/HttpServer.kt`. Clean `start()` / `stop()` API; `start()` spawns a daemon accept thread and returns immediately. `App.kt` is now a pure composition root exposing `buildServer()`. Foundation for all sub-items below. Prerequisite for DI lifecycle hooks.
  - [ ] **Bounded thread pool** — replace `Thread { }.start()` with a `ThreadPoolExecutor` (fixed pool size = max concurrent connections). Supersedes the current `Semaphore` approach. Prerequisite for all sub-items below. Feeds into section 8 Step 1 tuning.
  - [ ] **Bounded accept queue** — use `LinkedBlockingQueue(queueSize)` in `ThreadPoolExecutor`. The accept loop submits connections via `executor.execute {}` and never blocks — if the pool is full the queue absorbs the spike. When the queue is also full, `RejectedExecutionException` is thrown → send `503 Service Unavailable` and close. This solves the core dilemma: grace period for transient spikes *without* blocking the accept loop. Depends on bounded thread pool.
  - [ ] **Connection tracking** — maintain a count (or set) of active connections. Required to know when all in-flight requests have completed during drain. Depends on bounded thread pool.
  - [ ] **Accept backlog** — configure `ServerSocket.bind(addr, backlog)` explicitly. Default OS backlog (~50) drops SYN packets under burst traffic; set to 1024+ to absorb spikes without client-visible errors.
  - [x] **Idle connection timeout** — `socket.soTimeout = config.readTimeoutMs` on each accepted socket (set inside the worker thread). Evicts stale keep-alive connections, freeing threads and file descriptors. `SocketTimeoutException` caught silently in `HttpConnectionHandler`. Configurable via `HttpConfig.readTimeoutMs` (default 30s). Feeds into section 8 Step 1.
  - [ ] **Correct error close sequence** — after writing any error response (400, 404, 413, 500): `socket.shutdownOutput()` → drain remaining input → `socket.close()`. Prevents the OS from issuing a TCP RST that discards the response before the client reads it.
  - [ ] **Graceful shutdown on `SIGTERM`** — `Runtime.getRuntime().addShutdownHook`: close `ServerSocket` (stop accepting new connections), call `executor.shutdown()` + `awaitTermination()` (drain in-flight requests), then exit. Prevents file corruption mid-write in `WriteFileContent`. Depends on bounded thread pool + connection tracking.
- [x] **Typed config: `HttpConfig`** — `data class HttpConfig(port, maxConcurrentConnections, maxRequestBodyBytes, readTimeoutMs)` replaces all hardcoded constants. Passed into `buildServer()` / `HttpServer`. CLI args / env var parsing and remaining fields (accept queue depth) are pending.
- [ ] **Typed config: `FileConfig`** — base directory, max file size. Parsed from CLI args / env vars.
- [ ] **Middleware chain** — `fun interface Middleware { fun handle(request, next): HttpResponse }` — intercepts requests before handler for auth, rate limiting, tracing. `Router.buildChain()` uses `foldRight` to compose the chain. Prerequisite for sections 6 and 7.
- [ ] **DI container core** — manual `Container.kt` with `singleton {}` / `register {}` blocks and `get<T>()` resolution.
- [ ] **DI module system** — split wiring into focused modules (`httpModule()`, `fileModule()`, etc.) via `Container.() -> Unit` extension functions. Depends on DI container core.
- [ ] **Named DI bindings** — support multiple bindings of the same type (e.g., two `ExecutorService` instances for IO vs CPU pools). Depends on DI module system.
- [ ] **DI lifecycle hooks** — `start()` / `stop()` on managed resources (`ExecutorService`, `ServerSocket`, metrics reporters) for graceful startup and shutdown. Depends on `HttpServer` class + DI module system.

### 6. Security
- [ ] **Security response headers** — `X-Content-Type-Options: nosniff`, `X-Frame-Options`, `Referrer-Policy` as default enrichers. Trivial to add; no dependencies.
- [x] **Request timeout** — `socket.soTimeout` prevents slowloris by closing connections that stall during header/body reads. Configurable via `HttpConfig.readTimeoutMs` (default 30s). Per-route timeout not yet implemented.
- [ ] **Rate limiting** — per-IP request limit via `Middleware`. Returns `429 Too Many Requests`. Depends on Middleware chain (section 5).
- [ ] **Authentication** — `Middleware` returning `401 Unauthorized`. Bearer token extraction and validation. Depends on Middleware chain.
- [ ] **JWT validation** — signature verification and claims validation (expiry, audience). Separate from Bearer extraction. Depends on Authentication middleware.
- [ ] **TLS/HTTPS** — `SSLServerSocket`. TLS 1.2+ only. Required for HTTP/2 (ALPN). Most complex; enables the entire HTTP/2 roadmap.

### 7. Observability
- [ ] **Startup / shutdown log** — log server address and port when ready; log drain status on shutdown. Minimum operator visibility. No dependencies.
- [ ] **Health check endpoint** — `GET /health` → `200 OK` for load balancer probes. Trivial handler; needed from first production deploy.
- [ ] **Logging infrastructure** — add `slf4j` + `logback` dependency. Configure structured JSON output and log levels. Prerequisite for per-request access log.
- [ ] **Request correlation ID** — generate `X-Request-ID` per request; propagate in response headers. Prerequisite for log correlation and distributed tracing.
- [ ] **Per-request access log** — method, path, status, duration, bytes sent, correlation ID. Implemented as a `Middleware`. Depends on logging infra + correlation ID + Middleware chain.
- [ ] **Metrics** — request count, error rate, latency percentiles (p50/p99/p999), active connection count. Expose via `GET /metrics` (Prometheus). Implemented as a `Middleware`. Depends on logging infra + Middleware chain.
- [ ] **Distributed tracing** — propagate `traceparent` (W3C Trace Context), emit spans per request. Implemented as a `Middleware`. Depends on correlation ID + Middleware chain.

### 8. Performance & Concurrency

> Ceiling estimates for a simple plaintext endpoint on dedicated hardware.
> Evidence: [TechEmpower Round 23](https://www.techempower.com/benchmarks/), [ebarlas/java-httpserver-vthreads](https://github.com/ebarlas/java-httpserver-vthreads), [buffer-pooling case study](https://dev.to/uthman_dev/how-buffer-pooling-doubled-my-http-servers-throughput-4000-7721-rps-3i0g).
> Measure with `wrk` before and after each step to confirm the gain.

**Step 1 — ~150K RPS** *(current baseline, platform threads + blocking I/O)*
- [ ] **Thread pool tuning** — once `ThreadPoolExecutor` is in place (section 5), tune core/max pool size, queue depth, and keep-alive time based on `wrk` profiling. Depends on `HttpServer` class + `HttpConfig`.
- [ ] **Socket read timeout** — tune `setSoTimeout(ms)` value (introduced in section 5 `HttpServer`) based on load testing to balance idle connection eviction vs. legitimate slow clients.

**Step 2 — ~400K RPS** *(virtual threads)*
- [ ] **Virtual threads** — replace `Thread { }.start()` with `Executors.newVirtualThreadPerTaskExecutor()`. Minimal code change; JVM parks blocked threads without consuming an OS thread. Removes the platform-thread ceiling.

**Step 3 — ~1.5M RPS** *(NIO event loop)*
- [ ] **Non-blocking I/O** — migrate `HttpConnectionHandler` to Java NIO (`Selector` / `SocketChannel`) or Kotlin coroutines. Fixed thread pool; no thread blocked per connection. This is the architecture used by Netty, the TechEmpower top Java performer.
- [ ] **Zero-copy file serving** — `FileChannel.transferTo()` (OS `sendfile`) instead of `file.readBytes()` into heap.

**Step 4 — ~3M RPS** *(buffer and object pooling)*
- [ ] **Pooled I/O read buffers** — pre-allocate a `ByteArray` pool per thread. Reuse for `HttpRequestParser` instead of `new ByteArrayOutputStream()` per request. At 500K RPS the server discards ~32 MB/s of buffers, driving GC.
- [ ] **Pooled write buffers** — reuse serialisation buffers in `HttpResponseSerializer` instead of allocating a fresh array per response.
- [ ] **Pooled domain objects** — `HttpRequest` / `HttpResponse` / header map pooled and reset per request on the hot path. Avoids per-request allocation of short-lived objects.

**Step 5 — ~5–7M RPS** *(direct memory + GC + OS tuning)*
- [ ] **Direct memory for I/O** — `ByteBuffer.allocateDirect()` for socket reads/writes. Bytes pass kernel → userspace without touching the GC-managed heap.
- [ ] **GC: ZGC or Shenandoah** — sub-millisecond pause times. Default G1 pauses of 10–50 ms are fatal at multi-million RPS. Reference: [LinkedIn GC tuning](https://engineering.linkedin.com/garbage-collection/garbage-collection-optimization-high-throughput-and-low-latency-java-applications), [Uber JVM tuning](https://www.uber.com/blog/jvm-tuning-garbage-collection/).
- [ ] **Socket buffer tuning** — set `TCP_NODELAY` (disable Nagle), `SO_RCVBUF`, `SO_SNDBUF` on both `ServerSocket` and accepted `Socket` to match network throughput.
- [ ] **GraalVM Native Image** — AOT compilation: eliminates JVM warm-up (~3 ms startup vs ~100 ms JVM), reduces heap footprint, improves peak throughput on short-lived or latency-sensitive workloads.

### 9. Streaming & Large Data
- [ ] **Streaming response body** — introduce `Body` sealed type (`Body.Bytes` / `Body.Stream`) in domain. `HttpResponseSerializer` writes chunks. Required for files larger than available heap.
- [ ] **Chunked Transfer-Encoding (response)** — send `Transfer-Encoding: chunked` when `Content-Length` is unknown upfront. Depends on streaming response body.
- [ ] **Streaming request body** — expose request body as lazy `Sequence<ByteArray>` rather than a fully buffered `ByteArray`. Required for large uploads.
- [ ] **Server-Sent Events (SSE)** — `Body.ServerSentEvents(events: Flow<String>)`. Serializer holds connection open and flushes events as they arrive.

### 10. CI/CD
- [x] **GitHub Actions** — unit tests on every push and PR to `main`. Test reports uploaded as artifacts on pass and fail.
- [x] **ktlint** — `org.jlleitschuh.gradle.ktlint` plugin enforces Kotlin code style. Runs as a separate parallel workflow (`lint.yml`) alongside the test workflow (`test.yml`).
- [ ] **Docker image** — multi-stage build, minimal JRE base image (e.g. `eclipse-temurin:24-jre-alpine`). Prerequisite for VPS deployment.
- [ ] **VPS deployment** — systemd unit file, reverse proxy (nginx/caddy), firewall rules, OS tuning (`ulimit -n`, GC flags). Depends on Docker image.
- [ ] **`SO_REUSEPORT`** — socket option enabling multiple processes to bind the same port for zero-downtime rolling restarts. Depends on VPS deployment + `HttpServer` class.
