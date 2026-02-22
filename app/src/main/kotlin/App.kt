import adapters.HttpController
import application.RequestDispatcher
import infrastructure.http.HttpConnectionHandler
import infrastructure.fileSystem.LocalFileRepository
import java.net.InetSocketAddress
import java.net.ServerSocket

fun main(args: Array<String>) {
    val directoryPath = if (args.size > 1 && args[0] == "--directory") args[1] else "."

    val serverSocket = ServerSocket()
    serverSocket.reuseAddress = true
    serverSocket.bind(InetSocketAddress(4221))

    val localFileRepository = LocalFileRepository(directoryPath)
    val httpConnectionHandler = HttpConnectionHandler(HttpController(RequestDispatcher(localFileRepository)))
    while (true) { // keep server running
        val socket = serverSocket.accept()
        Thread { // initialize a platform thread
            socket.use { httpConnectionHandler.handle(it) }
        }.start()
    }
}
