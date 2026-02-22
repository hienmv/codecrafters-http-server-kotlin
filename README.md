# HTTP Server — Kotlin

[![progress-banner](https://backend.codecrafters.io/progress/http-server/0ebddf4f-0b34-4aca-85f1-4f8e1d9c1f38)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

Started as a solution to the [CodeCrafters "Build Your Own HTTP Server" challenge](https://app.codecrafters.io/courses/http-server/overview), then evolved into a study of Clean Architecture and DDD applied to a from-scratch HTTP/1.1 server — with the goal of building toward a server capable of handling **hundreds of thousands of requests per second**.

---

## Running

```sh
./your_program.sh                              # default directory: .
./your_program.sh --directory /path/to/files  # serve files from a custom directory
```

Listens on port `4221`.

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

---

## Architecture

Designed with **Clean Architecture** and **DDD** principles. Dependencies point strictly inward — inner layers know nothing about outer layers.

```
┌─────────────────────────────────────────────┐
│                  App.kt                     │  Composition root
└──────────────────────┬──────────────────────┘
                       │ wires
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
┌─────────────┐  ┌──────────┐  ┌─────────────────────┐
│  adapters/  │  │  infra/  │  │  infra/             │
│HttpController│  │  http/  │  │  fileSystem/        │
│ResponseWriter│  │         │  │LocalFileRepository  │
└──────┬──────┘  └─────────┘  └─────────────────────┘
       │ depends on
       ▼
┌─────────────────────────────────────────────┐
│               application/                 │
│  RequestDispatcher   HttpContext            │
│  FileRepository (port interface)           │
└──────────────────────┬──────────────────────┘
                       │ depends on
                       ▼
┌─────────────────────────────────────────────┐
│                  domain/                   │
│  HttpRequest   HttpResponse   HttpStatus   │
│  HttpMethod    HttpProtocol   HttpContentType│
│  HttpContentEncoding                       │
└─────────────────────────────────────────────┘
```

### Layer responsibilities

**`domain/`** — Pure data. Entities and value objects. No dependencies, no I/O.

**`application/`** — Use cases. `RequestDispatcher.dispatch(request): HttpResponse` is a pure function — no I/O, no side effects. `HttpContext` is a stateless response builder. `FileRepository` is a port interface (no `java.io`).

**`adapters/`** — Entry points. `HttpController` connects the dispatcher to the delivery mechanism. `ResponseWriter` is the delivery port interface.

**`infrastructure/http/`** — TCP and HTTP wire protocol. Parsing bytes into `HttpRequest`, serializing `HttpResponse` into bytes, managing TCP connection lifecycle and keep-alive.

**`infrastructure/fileSystem/`** — Local disk. Implements `FileRepository`. Owns path traversal protection, `java.io.File`.

**`App.kt`** — Composition root. The only place that knows all layers exist and wires them together.

### Key design decisions

- **`RequestDispatcher` is a pure function** — returns `HttpResponse`, never writes to a socket. Trivially testable with no mocks.
- **`ResponseWriter` lives in `adapters/`**, not `application/` — the application layer has no output ports, no I/O triggers.
- **`HttpContext` is a stateless builder** — each `result*` method creates a fresh headers map. No shared mutable state between calls.
- **`FileRepository` port** — the application layer never imports `java.io`. Swappable: local disk today, S3 or database tomorrow.
- **`HttpContentEncoding` in domain** — encoding names (`gzip`) are domain value objects shared between application (negotiation) and infrastructure (compression) without either depending on the other.

---

## Project Structure

```
app/src/main/kotlin/
├── domain/
│   ├── httpRequest/        HttpRequest, HttpMethod
│   ├── httpResponse/       HttpResponse, HttpStatus
│   └── vo/                 HttpContentType, HttpProtocol, HttpContentEncoding
├── application/
│   ├── FileRepository.kt   port interface (no java.io)
│   ├── HttpContext.kt      stateless response builder
│   └── RequestDispatcher.kt  pure application service
├── adapters/
│   ├── HttpController.kt   connects dispatch to delivery
│   └── ResponseWriter.kt   delivery port interface
├── infrastructure/
│   ├── http/               TCP + HTTP wire protocol
│   │   ├── HttpConnectionHandler.kt   connection lifecycle, keep-alive
│   │   ├── HttpRequestParser.kt       bytes → HttpRequest
│   │   ├── HttpResponseSerializer.kt  HttpResponse → bytes, GZIP
│   │   └── SocketResponseWriter.kt    writes bytes to socket
│   └── fileSystem/         local disk
│       └── LocalFileRepository.kt
└── App.kt                  composition root
```

---

## TODO — Toward a Production-Grade Server

### Performance / Concurrency

- [ ] **Virtual threads** — replace `Thread { }.start()` with `Executors.newVirtualThreadPerTaskExecutor()` (Java 21+). Eliminates thread-per-connection overhead, scales to hundreds of thousands of concurrent connections.
- [ ] **Non-blocking I/O** — migrate `HttpConnectionHandler` to Java NIO (`Selector`, `SocketChannel`) or Kotlin coroutines for async dispatch. Eliminates blocking on slow clients.
- [ ] **Thread pool tuning** — bounded thread pool with back-pressure for platform thread model. Prevent unbounded thread creation under load.
- [ ] **Socket read timeout** — `socket.setSoTimeout(ms)` to evict idle connections and free threads.
- [ ] **Zero-copy file serving** — use `FileChannel.transferTo()` (OS-level sendfile) for file responses instead of `readBytes()` into heap.

### Streaming & Large Data

- [ ] **Streaming response body** — introduce `Body` sealed type (`Body.Bytes` / `Body.Stream`) in domain. `HttpResponseSerializer` iterates chunks, never buffers the full file. Required for files > available heap.
- [ ] **Chunked Transfer-Encoding** — send `Transfer-Encoding: chunked` when `Content-Length` is unknown upfront (e.g., dynamically generated responses).
- [ ] **Streaming request body** — parse `Content-Length` and expose the request body as a lazy `Sequence<ByteArray>` rather than a fully buffered `ByteArray`. Required for large file uploads.
- [ ] **Server-Sent Events (SSE)** — add `Body.ServerSentEvents(events: Flow<String>)` to the `Body` sealed type. `HttpResponseSerializer` holds the connection open and flushes events as they arrive.

### HTTP Protocol

- [ ] **HTTP/1.1 pipelining** — handle multiple in-flight requests on the same connection without waiting for each response.
- [ ] **`100 Continue`** — respond to `Expect: 100-continue` before reading large request bodies.
- [ ] **Range requests** — `Range: bytes=0-1023` for partial file downloads (required for video streaming and resumable downloads).
- [ ] **Conditional requests** — `ETag`, `Last-Modified`, `If-None-Match`, `If-Modified-Since` for proper HTTP caching.
- [ ] **HEAD method** — respond with headers only, no body.
- [ ] **OPTIONS method** — CORS preflight support.
- [ ] **HTTP/2** — multiplexing, header compression (HPACK), server push.

### Correctness & Robustness

- [ ] **`data class` + `ByteArray`** — `HttpRequest.body` and `HttpResponse.body` use `ByteArray` in a `data class`, breaking `equals()`/`hashCode()`. Override both methods or use a regular `class`.
- [ ] **`IOException` on write** — `SocketResponseWriter.writeResponse()` has no try-catch. A client disconnect mid-response throws silently into `HttpConnectionHandler`'s catch block and sends a spurious 400 response to a closed socket.
- [ ] **Malformed request handling** — destructuring `requestLineParts` throws `IndexOutOfBoundsException` on malformed request lines. Should produce a proper `400 Bad Request` instead of propagating through the generic catch.
- [ ] **Header injection** — validate that header values set from user input (e.g., `User-Agent` echo) do not contain `\r\n` sequences.
- [ ] **Max request size** — enforce a limit on `Content-Length` to prevent memory exhaustion from malicious clients.
- [ ] **`Content-Length` validation** — if the client sends fewer bytes than declared, the body read loop exits early with a partial buffer. Detect and reject.

### Observability

- [ ] **Structured logging** — per-request log: method, path, status, duration, bytes sent. Use `slf4j` + `logback` with JSON output.
- [ ] **Metrics** — request count, error rate, latency percentiles (p50/p99/p999), active connection count. Expose via `/metrics` (Prometheus format).
- [ ] **Distributed tracing** — propagate `traceparent` (W3C Trace Context) header, emit spans per request.
- [ ] **Health check endpoint** — `GET /health` returning `200 OK` for load balancer probes.

### Security

- [ ] **TLS/HTTPS** — wrap `ServerSocket` with `SSLServerSocket`. Support TLS 1.2+ only.
- [ ] **Rate limiting** — per-IP request rate limit to prevent abuse.
- [ ] **Request timeout** — maximum time allowed for a complete request (headers + body) to prevent slowloris attacks.

### Extensibility

- [ ] **Middleware / filter chain** — before/after hooks around `RequestDispatcher.dispatch()`. Enables cross-cutting concerns (auth, logging, rate limiting) without modifying the dispatcher.
- [ ] **Route registration API** — instead of a hardcoded `when` block in `RequestDispatcher`, a registration-based router: `router.get("/echo/:text") { ctx -> ... }`.
- [ ] **Configuration** — port, directory, thread pool size, timeouts, TLS cert path — from environment variables or a config file, not hardcoded.
