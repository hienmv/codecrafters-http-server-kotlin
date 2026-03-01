package infrastructure.filesystem

import application.port.FileRepository
import java.io.File

class LocalFileRepository(private val directoryPath: String) : FileRepository {
    override fun read(fileName: String): ByteArray? {
        val file = resolve(fileName) ?: return null
        return if (file.exists()) file.readBytes() else null
    }

    override fun write(fileName: String, content: ByteArray): Boolean {
        val file = resolve(fileName) ?: return false
        file.writeBytes(content)
        return true
    }

    private fun resolve(fileName: String): File? {
        val basePath = File(directoryPath).canonicalPath
        val filePath = File(directoryPath, fileName).canonicalPath
        return if (filePath.startsWith(basePath + File.separator)) File(filePath) else null
    }
}