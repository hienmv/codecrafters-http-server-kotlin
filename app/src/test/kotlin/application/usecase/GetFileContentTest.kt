package application.usecase

import application.port.FileRepository
import domain.exception.ResourceNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GetFileContentTest : DescribeSpec({

    describe("GetFileContent") {

        it("returns bytes when the repository returns data") {
            // Arrange
            val expected = byteArrayOf(10, 20, 30)
            val repo = object : FileRepository {
                override fun read(fileName: String): ByteArray? = expected
                override fun write(fileName: String, content: ByteArray): Boolean = false
            }
            val useCase = GetFileContent(repo)
            // Act
            val result = useCase.execute("test.txt")
            // Assert
            result.toList() shouldBe expected.toList()
        }

        it("throws ResourceNotFoundException when the repository returns null") {
            // Arrange
            val repo = object : FileRepository {
                override fun read(fileName: String): ByteArray? = null
                override fun write(fileName: String, content: ByteArray): Boolean = false
            }
            val useCase = GetFileContent(repo)
            // Act & Assert
            val exception = shouldThrow<ResourceNotFoundException> {
                useCase.execute("missing.txt")
            }
            exception.message shouldBe "Resource not found: missing.txt"
        }
    }
})
