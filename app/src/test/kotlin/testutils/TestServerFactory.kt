package testutils

import buildServer
import infrastructure.http.HttpConfig
import infrastructure.http.HttpServer

object TestServerFactory {
    fun create(
        directoryPath: String = ".",
        maxRequestBodyBytes: Int = 10 * 1024 * 1024,
    ): HttpServer =
        buildServer(
            httpConfig = HttpConfig(port = 0, maxRequestBodyBytes = maxRequestBodyBytes, readTimeoutMs = 200),
            directoryPath = directoryPath,
        )
}
