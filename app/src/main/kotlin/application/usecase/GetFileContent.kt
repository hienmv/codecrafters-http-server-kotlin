package application.usecase

import application.port.FileRepository
import domain.exception.ResourceNotFoundException

class GetFileContent(
    private val fileRepository: FileRepository
) {
    fun execute(fileName: String): ByteArray {
        return fileRepository.read(fileName) ?: throw ResourceNotFoundException(fileName)
    }
}