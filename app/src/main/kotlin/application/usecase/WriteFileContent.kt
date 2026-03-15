package application.usecase

import application.port.FileRepository
import domain.exception.ResourceNotFoundException

class WriteFileContent(
    private val fileRepository: FileRepository,
) {
    fun execute(
        fileName: String,
        content: ByteArray,
    ) {
        if (!fileRepository.write(fileName, content)) {
            throw ResourceNotFoundException(fileName)
        }
    }
}
