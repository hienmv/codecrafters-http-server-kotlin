package application.usecase

import application.port.FileRepository
import domain.exception.WriteFailedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec

class WriteFileContentTest :
    DescribeSpec({

        describe("WriteFileContent") {

            it("completes without exception when the repository write succeeds") {
                // Arrange
                val repo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = null

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean = true
                    }
                val useCase = WriteFileContent(repo)
                // Act & Assert — no exception thrown
                useCase.execute("file.txt", byteArrayOf(1, 2, 3))
            }

            it("throws WriteFailedException when the repository write fails") {
                // Arrange
                val repo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = null

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean = false
                    }
                val useCase = WriteFileContent(repo)
                // Act & Assert
                shouldThrow<WriteFailedException> {
                    useCase.execute("file.txt", byteArrayOf(1, 2, 3))
                }
            }
        }
    })
