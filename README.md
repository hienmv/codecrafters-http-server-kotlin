# HTTP Server — Kotlin

[![progress-banner](https://backend.codecrafters.io/progress/http-server/0ebddf4f-0b34-4aca-85f1-4f8e1d9c1f38)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

Started as a solution to the [CodeCrafters "Build Your Own HTTP Server" challenge](https://app.codecrafters.io/courses/http-server/overview), then evolved into a study of **Hexagonal Architecture** and **DDD** applied to a from-scratch HTTP/1.1 server — with the goal of building toward a server capable of handling **hundreds of thousands of requests per second**.

---

## Running

```sh
./your_program.sh                              # default directory: .
./your_program.sh --directory /path/to/files  # serve files from a custom directory
```

Listens on port `4221`.

```sh
# run functional tests (requires: pip install requests)
python3 app/src/main/test.py
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
│  HttpController  │  │                  │  │  LocalFileRepository  │
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

**`App.kt`** — Composition root. The only place that knows all layers exist and wires them together.

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
│   └── vo/                 HttpProtocol, HttpContentType, HttpContentEncoding
├── application/
│   ├── port/
│   │   └── FileRepository.kt        outbound port — no java.io
│   └── usecase/
│       ├── GetFileContent.kt         (fileName): ByteArray?
│       └── WriteFileContent.kt       (fileName, content): Boolean
├── adapters/
│   └── http/
│       ├── handler/                  API definitions — translate HTTP ↔ use case
│       │   ├── RootHandler.kt
│       │   ├── EchoHandler.kt
│       │   ├── UserAgentHandler.kt
│       │   └── FileHandler.kt
│       ├── Router.kt                 path param routing, get/post registration
│       ├── HttpContext.kt            stateless response builder + pathParam()
│       ├── HttpController.kt         bridges infrastructure → router
│       └── ResponseWriter.kt         output port interface
├── infrastructure/
│   ├── http/
│   │   ├── HttpConnectionHandler.kt  connection lifecycle, keep-alive
│   │   ├── HttpRequestParser.kt      bytes → HttpRequest
│   │   ├── HttpResponseSerializer.kt HttpResponse → bytes, GZIP
│   │   └── SocketResponseWriter.kt   implements ResponseWriter
│   └── fileSystem/
│       └── LocalFileRepository.kt    implements FileRepository, path traversal protection
└── App.kt                            composition root
```

---

## Done

### Architecture
- [x] **Hexagonal / Clean Architecture** — strict layer separation: domain → application → adapters → infrastructure. Dependencies point inward only.
- [x] **DDD** — use cases (`GetFileContent`, `WriteFileContent`) in application layer. Handlers are HTTP-specific adapters, not use cases.
- [x] **`FileRepository` port (DIP)** — application layer defines the interface; infrastructure implements it. No `java.io` in application or domain.
- [x] **Handlers as API definitions** — live in `adapters/http/handler/`, translate HTTP ↔ use case. Use cases remain HTTP-agnostic.
- [x] **`HttpContext` stateless builder** — each `result*` method creates a fresh headers map. No shared mutable state between calls. Carries `pathParams`.
- [x] **`SO_REUSEADDR` before bind** — `ServerSocket()` created unbound, `reuseAddress = true` set, then `bind()` called.

### Routing
- [x] **Registration-based router** — `Router.get(path, handler)` / `Router.post(path, handler)` replaces hardcoded `when` block.
- [x] **Path parameter extraction** — `{param}` in route patterns (e.g. `/echo/{text}`, `/files/{fileName}`). `Router.matchPath()` splits pattern and path by `/`, extracts named segments. Injected into `HttpContext`, accessed via `ctx.pathParam("name")`.

### HTTP/1.1
- [x] **Persistent connections (keep-alive)** — connection loop in `HttpConnectionHandler` reuses TCP connection across multiple requests.
- [x] **`Connection: close` graceful shutdown** — server detects header, sends `Connection: close` in response, closes after reply.
- [x] **`Accept-Encoding: gzip` negotiation** — `HttpContext.build()` checks accepted encodings; `HttpResponseSerializer` compresses body with `GZIPOutputStream`.
- [x] **Binary-safe bodies** — `HttpRequest.body` and `HttpResponse.body` are `ByteArray` throughout. No UTF-8 corruption of binary files.
- [x] **Path traversal protection** — `LocalFileRepository` resolves canonical paths and rejects any path escaping the base directory.

### Testing
- [x] **Functional test suite** (`test.py`) — covers all routes, keep-alive, wrong/unsupported encodings, `Connection: close`, 20 concurrent requests, mixed route load.
- [x] **Unit tests (Kotest)** — 72 tests across all layers using `DescribeSpec` + AAA style. No mocking library: outbound ports faked with anonymous objects. Covers `Router` (path matching, param extraction, enrichers), all handlers, use cases, `HttpRequestParser`, `HttpResponseSerializer`, enrichers, error handlers, `LocalFileRepository` (including path traversal), and `HttpProtocol`.

---

## TODO — Toward a Production-Grade Server

### Architecture & Extensibility

- [ ] **Middleware chain** — `fun interface Middleware { fun handle(request, next): HttpResponse }` — intercepts requests before handler for auth, rate limiting, tracing. Supports both global and per-route middleware. `Router.buildChain()` uses `foldRight` to compose the chain.
- [ ] **Response enrichers** — `fun interface ResponseEnricher { fun enrich(request, response): HttpResponse }` — applies cross-cutting response headers (Content-Encoding, Connection) after handler returns. Moves protocol concerns out of `HttpContext` into `infrastructure/http/enricher/`.
- [ ] **DI container** — manual `Container.kt` with `singleton {}` and `register {}` blocks. Provider lambdas take `Container` as receiver, enabling `get<T>()` inside providers for chained resolution. Split into focused modules (`httpModule()`, `routerModule()`, etc.) via `Container.() -> Unit` extension functions.
- [ ] **Named DI bindings** — support multiple bindings of the same type (e.g., two `ExecutorService` instances for IO vs CPU).
- [ ] **DI lifecycle hooks** — `start()` / `stop()` on managed resources (`ExecutorService`, `ServerSocket`, metrics reporters) for graceful shutdown.
- [ ] **`AppConfig` / `HttpConfig` / `FileConfig`** — typed config classes per layer. `AppConfig` parsed from CLI args / env vars in `App.kt`. `HttpConfig` owns port, soTimeout, maxRequestSize. `FileConfig` owns directoryPath, maxFileSize. Registered as singletons in the DI container.
- [ ] **`HttpServer` class** — encapsulate TCP lifecycle (`ServerSocket` creation, bind, accept loop, thread management, `start()` / `stop()`) out of `App.kt` into `infrastructure/http/HttpServer.kt`.

### Performance / Concurrency

- [ ] **Virtual threads** — replace `Thread { }.start()` with `Executors.newVirtualThreadPerTaskExecutor()` (Java 21+). Eliminates thread-per-connection overhead, scales to hundreds of thousands of concurrent connections.
- [ ] **Non-blocking I/O** — migrate `HttpConnectionHandler` to Java NIO (`Selector`, `SocketChannel`) or Kotlin coroutines for async dispatch. Eliminates blocking on slow clients.
- [ ] **Thread pool tuning** — bounded thread pool with back-pressure for platform thread model. Prevent unbounded thread creation under load.
- [ ] **Socket read timeout** — `socket.setSoTimeout(ms)` to evict idle connections and free threads.
- [ ] **File descriptor limits** — `ulimit -n` to increase max open files. Default 1024 limits concurrent connections.
- [ ] **Zero-copy file serving** — use `FileChannel.transferTo()` (OS-level sendfile) for file responses instead of `readBytes()` into heap.

### Streaming & Large Data

- [ ] **Streaming response body** — introduce `Body` sealed type (`Body.Bytes` / `Body.Stream`) in domain. `HttpResponseSerializer` iterates chunks, never buffers the full file. Required for files > available heap.
- [ ] **Chunked Transfer-Encoding** — send `Transfer-Encoding: chunked` when `Content-Length` is unknown upfront.
- [ ] **Streaming request body** — parse `Content-Length` and expose the request body as a lazy `Sequence<ByteArray>` rather than a fully buffered `ByteArray`. Required for large file uploads.
- [ ] **Server-Sent Events (SSE)** — add `Body.ServerSentEvents(events: Flow<String>)` to the `Body` sealed type. `HttpResponseSerializer` holds the connection open and flushes events as they arrive.

### HTTP Protocol

- [ ] **HTTP/1.1 pipelining** — handle multiple in-flight requests on the same connection without waiting for each response.
- [ ] **`100 Continue`** — respond to `Expect: 100-continue` before reading large request bodies.
- [ ] **Range requests** — `Range: bytes=0-1023` for partial file downloads (video streaming, resumable downloads).
- [ ] **Conditional requests** — `ETag`, `Last-Modified`, `If-None-Match`, `If-Modified-Since` for proper HTTP caching.
- [ ] **HEAD method** — respond with headers only, no body.
- [ ] **OPTIONS method** — CORS preflight support.
- [ ] **HTTP/2** — multiplexing, header compression (HPACK), server push.

### Correctness & Robustness

- [ ] **`data class` + `ByteArray`** — `HttpRequest.body` and `HttpResponse.body` use `ByteArray` in a `data class`, breaking `equals()`/`hashCode()`. Override both or use a regular `class`.
- [ ] **`IOException` on write** — `SocketResponseWriter.writeResponse()` has no try-catch. A client disconnect mid-response throws silently into `HttpConnectionHandler`'s catch block and sends a spurious 400 to a closed socket.
- [ ] **Malformed request handling** — destructuring `requestLineParts` throws `IndexOutOfBoundsException` on malformed request lines. Should produce a proper `400 Bad Request`.
- [ ] **Header injection** — validate that header values set from user input (e.g., `User-Agent` echo) do not contain `\r\n` sequences.
- [ ] **Max request size** — enforce a limit on `Content-Length` to prevent memory exhaustion from malicious clients.
- [ ] **`Content-Length` validation** — if the client sends fewer bytes than declared, detect and reject the partial buffer.

### Observability

- [ ] **Structured logging** — per-request log: method, path, status, duration, bytes sent. Implement as a `Middleware` using `slf4j` + `logback` with JSON output.
- [ ] **Metrics** — request count, error rate, latency percentiles (p50/p99/p999), active connection count. Expose via `GET /metrics` (Prometheus format). Implement as a `Middleware`.
- [ ] **Distributed tracing** — propagate `traceparent` (W3C Trace Context) header, emit spans per request. Implement as a `Middleware`.
- [ ] **Health check endpoint** — `GET /health` returning `200 OK` for load balancer probes. One line in `App.kt`.

### Security

- [ ] **TLS/HTTPS** — wrap `ServerSocket` with `SSLServerSocket`. Support TLS 1.2+ only. Configurable via `HttpConfig`.
- [ ] **Authentication** — `Middleware` returning `401 Unauthorized` before the handler is reached. Supports Bearer token, API key, or JWT validation.
- [ ] **Rate limiting** — per-IP request rate limit via `Middleware`. Returns `429 Too Many Requests`.
- [ ] **Request timeout** — maximum time for a complete request (headers + body) to prevent slowloris attacks. Configurable via `HttpConfig`.

### Testing

- [x] **Unit tests** — use cases (`GetFileContent`, `WriteFileContent`) are pure functions with no mocks needed. Handlers testable by constructing `HttpContext` directly.
- [ ] **Integration tests** — start the server in a test process, send real HTTP requests via `test.py` or a JVM test client.
- [ ] **Performance benchmarks** — JMH for `Router.dispatch()` and `HttpResponseSerializer`. Load testing with `wrk` or `hey`.
- [ ] **Fuzz testing** — send malformed requests to ensure `400 Bad Request` and no crashes.
- [ ] **Security testing** — path traversal, header injection, slowloris, DoS attack vectors.

### CI/CD

- [x] **GitHub Actions** — unit tests run on every push and PR to `main` via `.github/workflows/test.yml`. Test reports uploaded as artifacts on both pass and fail.
- [ ] **Docker image** — multi-stage build, minimal JRE base image.
- [ ] **Deployment** — deploy to a self-hosted VPS. Zero-downtime via `SO_REUSEPORT` + rolling restart.
