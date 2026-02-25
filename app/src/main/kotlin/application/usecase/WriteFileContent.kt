package application.usecase

import application.port.FileRepository

class WriteFileContent(
    private val fileRepository: FileRepository
) {
    fun execute(fileName: String, content: ByteArray): Boolean {
        return fileRepository.write(fileName, content)
    }
}