package adapter.http.handler

import adapter.http.HttpContext
import application.port.FileRepository
import application.usecase.WriteFileContent
import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpStatus
import domain.vo.HttpProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class WriteFileContentHandlerTest : DescribeSpec({

    describe("WriteFileContentHandler") {

        it("writes request body to repository and returns 201 Created") {
            // Arrange
            val bodyBytes = "file content".toByteArray()
            var writtenFileName = ""
            var writtenContent = byteArrayOf()
            val fakeRepo = object : FileRepository {
                override fun read(fileName: String): ByteArray? = null
                override fun write(fileName: String, content: ByteArray): Boolean {
                    writtenFileName = fileName
                    writtenContent = content
                    return true
                }
            }
            val handler = WriteFileContentHandler(WriteFileContent(fakeRepo))
            val ctx = HttpContext(
                request = HttpRequest(
                    method = HttpMethod.POST,
                    target = "/files/test.txt",
                    protocol = HttpProtocol.HTTP11,
                    body = bodyBytes,
                ),
                pathParams = mapOf("fileName" to "test.txt"),
            )
            // Act
            val response = handler.create(ctx)
            // Assert
            response.status shouldBe HttpStatus.CREATED_201
            writtenFileName shouldBe "test.txt"
            writtenContent.toList() shouldBe bodyBytes.toList()
        }
    }
})
