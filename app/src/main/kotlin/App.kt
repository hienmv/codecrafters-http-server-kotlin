import java.net.ServerSocket;
import java.util.Scanner

fun main() {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    println("Logs from your program will appear here!")

    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    while (true) { // keep server running
        serverSocket.accept().use { client ->
            val input = Scanner(client.getInputStream())
            val outputStream = client.getOutputStream()

            val startLine = input.nextLine()
            val response =
                if (startLine == "GET / HTTP/1.1") {
                    "HTTP/1.1 200 OK\r\n\r\n"
                } else {
                    "HTTP/1.1 404 Not Found\r\n\r\n"
                }
            outputStream.write(response.toByteArray())
            outputStream.flush()
        }
    }
}
