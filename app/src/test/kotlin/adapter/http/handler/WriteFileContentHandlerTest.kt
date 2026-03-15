package adapter.http.handler

import adapter.http.HttpContext
import application.port.FileRepository
import application.usecase.WriteFileContent
import domain.exception.ResourceNotFoundException
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WriteFileContentHandlerTest :
    DescribeSpec({

        fun makeContext(
            fileName: String,
            body: ByteArray,
        ) = HttpContext(
            request =
                HttpRequest(
                    method = HttpMethod.POST,
                    target = "/files/$fileName",
                    protocol = HttpProtocol.HTTP11,
                    body = body,
                ),
            pathParams = mapOf("fileName" to fileName),
        )

        describe("WriteFileContentHandler") {

            it("writes request body to repository and returns 201 Created") {
                // Arrange
                val bodyBytes = "file content".toByteArray()
                var writtenFileName = ""
                var writtenContent = byteArrayOf()
                val fakeRepo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = null

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean {
                            writtenFileName = fileName
                            writtenContent = content
                            return true
                        }
                    }
                val handler = WriteFileContentHandler(WriteFileContent(fakeRepo))
                // Act
                val response = handler.create(makeContext("test.txt", bodyBytes))
                // Assert
                response.status shouldBe HttpStatus.CREATED_201
                writtenFileName shouldBe "test.txt"
                writtenContent.toList() shouldBe bodyBytes.toList()
            }

            it("propagates ResourceNotFoundException when the repository write fails") {
                // Arrange
                val fakeRepo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = null

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean = false
                    }
                val handler = WriteFileContentHandler(WriteFileContent(fakeRepo))
                // Act & Assert
                shouldThrow<ResourceNotFoundException> {
                    handler.create(makeContext("test.txt", byteArrayOf(1, 2, 3)))
                }
            }
        }
    })
