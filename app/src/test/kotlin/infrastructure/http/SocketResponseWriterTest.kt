package infrastructure.http

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.Socket

class SocketResponseWriterTest :
    DescribeSpec({
        describe("SocketResponseWriter") {

            fun makeSocket(out: OutputStream): Socket =
                object : Socket() {
                    override fun getOutputStream() = out
                }

            it("writes the serialized response to the output stream") {
                // Arrange
                val buffer = ByteArrayOutputStream()
                val writer = SocketResponseWriter(makeSocket(buffer))
                val response = HttpResponse(status = HttpStatus.OK_200)
                // Act
                writer.writeResponse(response)
                // Assert
                buffer.toByteArray().toList() shouldBe HttpResponseSerializer.serialize(response).toList()
            }

            it("does not propagate IOException when writing fails") {
                // Arrange
                val brokenOut =
                    object : OutputStream() {
                        override fun write(b: Int) = throw IOException("Broken pipe")

                        override fun write(
                            b: ByteArray,
                            off: Int,
                            len: Int,
                        ) = throw IOException("Broken pipe")
                    }
                val writer = SocketResponseWriter(makeSocket(brokenOut))
                val response = HttpResponse(status = HttpStatus.OK_200)
                // Act + Assert
                shouldNotThrowAny { writer.writeResponse(response) }
            }

            it("propagates non-IOException exceptions") {
                // Arrange
                val brokenOut =
                    object : OutputStream() {
                        override fun write(b: Int) = throw RuntimeException("unexpected error")

                        override fun write(
                            b: ByteArray,
                            off: Int,
                            len: Int,
                        ) = throw RuntimeException("unexpected error")
                    }
                val writer = SocketResponseWriter(makeSocket(brokenOut))
                val response = HttpResponse(status = HttpStatus.OK_200)
                // Act + Assert
                shouldThrow<RuntimeException> { writer.writeResponse(response) }
            }

            it("closes the output stream on IOException") {
                // Arrange
                var closed = false
                val brokenOut =
                    object : OutputStream() {
                        override fun write(b: Int) = throw IOException("Broken pipe")

                        override fun write(
                            b: ByteArray,
                            off: Int,
                            len: Int,
                        ) = throw IOException("Broken pipe")

                        override fun close() {
                            closed = true
                        }
                    }
                val writer = SocketResponseWriter(makeSocket(brokenOut))
                val response = HttpResponse(status = HttpStatus.OK_200)
                // Act
                writer.writeResponse(response)
                // Assert
                closed.shouldBeTrue()
            }
        }
    })
