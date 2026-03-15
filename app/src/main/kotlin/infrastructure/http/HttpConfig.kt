package infrastructure.http

data class HttpConfig(
    val port: Int = 4221,
    val maxConcurrentConnections: Int = 100,
    val maxRequestBodyBytes: Int = 10 * 1024 * 1024, // 10 MB,
    val readTimeoutMs: Int = 30_000, // 30 seconds
)
