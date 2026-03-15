package infrastructure.http

import adapter.http.port.HttpAdapter
import adapter.http.port.HttpErrorHandler
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class HttpServer(
    private val config: HttpConfig,
    private val adapter: HttpAdapter,
    private val errorHandler: HttpErrorHandler,
) {
    private lateinit var serverSocket: ServerSocket

    private val connectionHandler =
        HttpConnectionHandler(
            adapter = adapter,
            errorHandler = errorHandler,
            maxRequestBodyBytes = config.maxRequestBodyBytes,
        )

    val port: Int get() = serverSocket.localPort

    fun start() {
        check(!::serverSocket.isInitialized) { "Server is already running" }

        serverSocket =
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(config.port))
            }

        val semaphore = Semaphore(config.maxConcurrentConnections)
        Thread {
            while (true) { // keep server running
                val socket =
                    try {
                        serverSocket.accept() // wait for a client to connect
                    } catch (e: SocketException) {
                        if (!serverSocket.isClosed) {
                            println("Error accepting connection: ${e.message}")
                        }
                        break
                    }

                if (!semaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    // reject the connection if we are at capacity and cannot acquire a slot within the timeout
                    HttpConnectionLimitHandler.reject(socket)
                    continue
                }
                Thread {
                    if (config.readTimeoutMs != 0) {
                        //  a read() call on the InputStream associated with this Socket will block for only this amount of time.
                        //  If the timeout expires, a java.net.SocketTimeoutException is raised, though the Socket is still valid.
                        socket.soTimeout = config.readTimeoutMs
                    }
                    // initialize a platform thread
                    socket.use { connectionHandler.handle(it) }
                    // release the slot for new connections once the client disconnects and the socket is closed
                    semaphore.release()
                }.also { it.isDaemon = true }.start()
            }
        }.also { it.isDaemon = true }.start()
    }

    fun stop() {
        check(::serverSocket.isInitialized) { "Server is not running" }
        if (!serverSocket.isClosed) {
            serverSocket.close()
        }
    }
}
