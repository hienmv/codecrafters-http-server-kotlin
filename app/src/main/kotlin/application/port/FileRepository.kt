package application.port

interface FileRepository {
    // return null if the file not found
    fun read(fileName: String): ByteArray?

    // return false if failed to write
    fun write(
        fileName: String,
        content: ByteArray,
    ): Boolean
}
