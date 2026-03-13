import adapter.http.HttpRequestAdapter
import adapter.http.Router
import adapter.http.handler.EchoHandler
import adapter.http.handler.GetFileContentHandler
import adapter.http.handler.RootHandler
import adapter.http.handler.UserAgentHandler
import adapter.http.handler.WriteFileContentHandler
import application.usecase.GetFileContent
import application.usecase.WriteFileContent
import domain.exception.PayloadTooLargeException
import domain.exception.ResourceNotFoundException
import infrastructure.filesystem.LocalFileRepository
import infrastructure.http.HttpConfig
import infrastructure.http.HttpServer
import infrastructure.http.enricher.ConnectionHeaderEnricher
import infrastructure.http.enricher.GzipEncodingEnricher
import infrastructure.http.error.CompositeHttpErrorHandler
import infrastructure.http.error.FallbackErrorHandler
import infrastructure.http.error.InvalidRequestErrorHandler
import infrastructure.http.error.NotFoundErrorHandler
import infrastructure.http.error.PayloadTooLargeRequestErrorHandler

fun main(args: Array<String>) {
    val directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."
    val server = buildServer(directoryPath = directoryPath)

    // spawns daemon thread, returns immediately
    server.start()
    // keep the main thread alive to prevent the application from exiting
    // the main thread parks here forever while the server is running
    Thread.currentThread().join()
}

fun buildServer(
    httpConfig: HttpConfig = HttpConfig(),
    directoryPath: String = ".",
): HttpServer {
    val localFileRepository = LocalFileRepository(directoryPath)
    val getFileContentHandler = GetFileContentHandler(GetFileContent(localFileRepository))
    val writeFileContentHandler = WriteFileContentHandler(WriteFileContent(localFileRepository))
    val router =
        Router(enrichers = listOf(ConnectionHeaderEnricher, GzipEncodingEnricher))
            .get("/", RootHandler::get)
            .get("/user-agent", UserAgentHandler::get)
            .get("/echo/{text}", EchoHandler::get)
            .get("/files/{fileName}", getFileContentHandler::get)
            .post("/files/{fileName}", writeFileContentHandler::create)

    return HttpServer(
        config = httpConfig,
        adapter = HttpRequestAdapter(router),
        errorHandler =
            CompositeHttpErrorHandler(
                handlers =
                    listOf(
                        ResourceNotFoundException::class to NotFoundErrorHandler,
                        IllegalArgumentException::class to InvalidRequestErrorHandler,
                        PayloadTooLargeException::class to PayloadTooLargeRequestErrorHandler,
                    ),
                fallbackHandler = FallbackErrorHandler,
            ),
    )
}
