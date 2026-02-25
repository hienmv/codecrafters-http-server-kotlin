package application.usecase

import application.port.FileRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WriteFileContentTest : DescribeSpec({

    describe("WriteFileContent") {

        it("returns true when the repository write succeeds") {
            // Arrange
            val repo = object : FileRepository {
                override fun read(fileName: String): ByteArray? = null
                override fun write(fileName: String, content: ByteArray): Boolean = true
            }
            val useCase = WriteFileContent(repo)
            // Act
            val result = useCase.execute("file.txt", byteArrayOf(1, 2, 3))
            // Assert
            result shouldBe true
        }

        it("returns false when the repository write fails") {
            // Arrange
            val repo = object : FileRepository {
                override fun read(fileName: String): ByteArray? = null
                override fun write(fileName: String, content: ByteArray): Boolean = false
            }
            val useCase = WriteFileContent(repo)
            // Act
            val result = useCase.execute("file.txt", byteArrayOf(1, 2, 3))
            // Assert
            result shouldBe false
        }
    }
})
