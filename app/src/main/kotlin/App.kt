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
import infrastructure.http.HttpConnectionHandler
import infrastructure.http.HttpConnectionLimitHandler
import infrastructure.http.enricher.ConnectionHeaderEnricher
import infrastructure.http.enricher.GzipEncodingEnricher
import infrastructure.http.error.CompositeHttpErrorHandler
import infrastructure.http.error.FallbackErrorHandler
import infrastructure.http.error.InvalidRequestErrorHandler
import infrastructure.http.error.NotFoundErrorHandler
import infrastructure.http.error.PayloadTooLargeRequestErrorHandler
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

const val MAX_CONCURRENT_CONNECTIONS = 100

fun main(args: Array<String>) {
    val directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."

    val serverSocket = ServerSocket()
    serverSocket.reuseAddress = true
    serverSocket.bind(InetSocketAddress(4221))

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

    val errorHandler =
        CompositeHttpErrorHandler(
            handlers =
                listOf(
                    ResourceNotFoundException::class to NotFoundErrorHandler,
                    IllegalArgumentException::class to InvalidRequestErrorHandler,
                    PayloadTooLargeException::class to PayloadTooLargeRequestErrorHandler,
                ),
            fallbackHandler = FallbackErrorHandler,
        )
    val httpConnectionHandler =
        HttpConnectionHandler(
            adapter = HttpRequestAdapter(router),
            errorHandler = errorHandler,
        )

    val semaphore = Semaphore(MAX_CONCURRENT_CONNECTIONS)
    while (true) { // keep server running
        val socket = serverSocket.accept() // wait for a client to connect
        if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
            // reject the connection if we are at capacity and cannot acquire a slot within the timeout
            HttpConnectionLimitHandler.reject(socket)
            continue
        }
        Thread {
            // initialize a platform thread
            socket.use { httpConnectionHandler.handle(it) }
            // release the slot for new connections once the client disconnects and the socket is closed
            semaphore.release()
        }.start()
    }
}
