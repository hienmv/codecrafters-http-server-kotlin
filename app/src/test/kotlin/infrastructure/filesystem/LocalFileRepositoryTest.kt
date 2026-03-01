package infrastructure.filesystem

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.IOException
import java.nio.file.Files

class LocalFileRepositoryTest : DescribeSpec({

    val tempDir = Files.createTempDirectory("local-file-repo-test").toFile()

    afterSpec {
        tempDir.deleteRecursively()
    }

    describe("LocalFileRepository") {

        describe("read") {

            it("reads an existing file and returns its bytes") {
                // Arrange
                val file = tempDir.resolve("hello.txt")
                file.writeBytes("hello".toByteArray())
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.read("hello.txt")
                // Assert
                String(result!!, Charsets.UTF_8) shouldBe "hello"
            }

            it("returns null for a non-existent file") {
                // Arrange
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.read("nonexistent.txt")
                // Assert
                result.shouldBeNull()
            }

            it("rejects a path traversal attempt and returns null") {
                // Arrange
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.read("../secret")
                // Assert
                result.shouldBeNull()
            }

            it("rejects a fileName that resolves to the base directory itself") {
                // Arrange
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.read("")
                // Assert
                result.shouldBeNull()
            }
        }

        describe("write") {

            it("writes a file successfully and the content can be read back") {
                // Arrange
                val content = "written content".toByteArray()
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val writeResult = repo.write("output.txt", content)
                val readResult = repo.read("output.txt")
                // Assert
                writeResult shouldBe true
                readResult!!.toList() shouldBe content.toList()
            }

            it("rejects a path traversal attempt and returns false") {
                // Arrange
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.write("../escape.txt", "data".toByteArray())
                // Assert
                result shouldBe false
            }

            it("rejects a fileName that resolves to the base directory itself and returns false") {
                // Arrange
                val repo = LocalFileRepository(tempDir.absolutePath)
                // Act
                val result = repo.write("", "data".toByteArray())
                // Assert
                result shouldBe false
            }

            it("propagates IOException when the file cannot be written") {
                // Note: may not be reliable when running as root (root ignores read-only permissions)
                // Arrange
                val readOnlyDir = Files.createTempDirectory("read-only-test").toFile()
                readOnlyDir.setReadOnly()
                val repo = LocalFileRepository(readOnlyDir.absolutePath)
                // Act & Assert
                try {
                    shouldThrow<IOException> {
                        repo.write("file.txt", "data".toByteArray())
                    }
                } finally {
                    readOnlyDir.setWritable(true)
                    readOnlyDir.deleteRecursively()
                }
            }
        }
    }
})
