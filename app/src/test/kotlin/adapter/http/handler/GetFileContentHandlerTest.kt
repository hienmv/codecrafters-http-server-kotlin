package adapter.http.handler

import adapter.http.HttpContext
import application.port.FileRepository
import application.usecase.GetFileContent
import domain.exception.ResourceNotFoundException
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GetFileContentHandlerTest :
    DescribeSpec({

        fun makeContext(fileName: String) =
            HttpContext(
                request =
                    HttpRequest(
                        method = HttpMethod.GET,
                        target = "/files/$fileName",
                        protocol = HttpProtocol.HTTP11,
                        body = byteArrayOf(),
                    ),
                pathParams = mapOf("fileName" to fileName),
            )

        describe("GetFileContentHandler") {

            it("returns 200 with file bytes when the file exists") {
                // Arrange
                val fileBytes = byteArrayOf(1, 2, 3)
                val fakeRepo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = fileBytes

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean = false
                    }
                val handler = GetFileContentHandler(GetFileContent(fakeRepo))
                val ctx = makeContext("test.txt")
                // Act
                val response = handler.get(ctx)
                // Assert
                response.status shouldBe HttpStatus.OK_200
                response.body!!.toList() shouldBe fileBytes.toList()
            }

            it("propagates ResourceNotFoundException when the file does not exist") {
                // Arrange
                val fakeRepo =
                    object : FileRepository {
                        override fun read(fileName: String): ByteArray? = null

                        override fun write(
                            fileName: String,
                            content: ByteArray,
                        ): Boolean = false
                    }
                val handler = GetFileContentHandler(GetFileContent(fakeRepo))
                val ctx = makeContext("missing.txt")
                // Act & Assert
                shouldThrow<ResourceNotFoundException> {
                    handler.get(ctx)
                }
            }
        }
    })
