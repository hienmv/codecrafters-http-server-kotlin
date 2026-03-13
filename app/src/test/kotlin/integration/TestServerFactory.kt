package integration

import buildServer
import infrastructure.http.HttpConfig
import infrastructure.http.HttpServer

object TestServerFactory {
    fun create(directoryPath: String = "."): HttpServer =
        buildServer(
            httpConfig = HttpConfig(port = 0),
            directoryPath = directoryPath,
        )
}
