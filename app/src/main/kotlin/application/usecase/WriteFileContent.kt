package application.usecase

import application.port.FileRepository
import domain.exception.WriteFailedException

class WriteFileContent(
    private val fileRepository: FileRepository,
) {
    fun execute(
        fileName: String,
        content: ByteArray,
    ) {
        if (!fileRepository.write(fileName, content)) {
            throw WriteFailedException(fileName)
        }
    }
}
