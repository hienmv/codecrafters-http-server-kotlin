# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a [CodeCrafters](https://codecrafters.io) challenge implementing an HTTP/1.1 server from scratch in Kotlin, without any HTTP framework dependencies.

## Commands

**Build:**
```bash
gradle distTar
```

**Run:**
```bash
./your_program.sh [--directory <path>]
```
The `--directory` flag specifies the root directory for file serving (defaults to current directory).

**Run tests (CodeCrafters):**
Tests are run by the CodeCrafters platform on `git push`. There is no local test suite.

## Architecture

All source is in `app/src/main/kotlin/`. The project uses Java 24 via Gradle toolchain.

**Entry point:** `App.kt` — starts a `ServerSocket` on port 4221, accepts clients in an infinite loop, and spawns a platform thread per client via `handle()`.

**Request lifecycle:**
1. `HttpRequest.parse()` reads from the socket's `InputStream`, parses the request line, headers, and body (based on `Content-Length`)
2. `handle()` loops over persistent connections until the client sends `Connection: close`
3. `handleRequest()` routes by path and returns an `HttpResponse`
4. `HttpResponse.toBytes()` serializes to wire format, applying GZIP if `Accept-Encoding: gzip` was requested

**Routing (in `handleRequest`):**
- `GET /` → 200 OK
- `GET /echo/{text}` → echoes text, supports GZIP compression
- `GET /user-agent` → returns the `User-Agent` header value
- `GET /files/{filename}` → reads file from configured directory
- `POST /files/{filename}` → writes request body to file in configured directory

**Package layout:**
- `domain.httpRequest/` — `HttpRequest`, `HttpMethod`
- `domain.httpResponse/` — `HttpResponse`, `HttpStatus`
- `domain.vo/` — `Constants`, `HttpContentType`, `HttpProtocol`

**Global config:** A singleton `Config` object in `App.kt` holds the `--directory` value; it is set once at startup and read-only thereafter.
