package domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HttpProtocolTest : DescribeSpec({

    describe("HttpProtocol.fromValue") {

        it("returns HTTP11 for 'HTTP/1.1'") {
            // Arrange & Act
            val result = HttpProtocol.fromValue("HTTP/1.1")
            // Assert
            result shouldBe HttpProtocol.HTTP11
        }

        it("returns HTTP1 for 'HTTP/1.0'") {
            // Arrange & Act
            val result = HttpProtocol.fromValue("HTTP/1.0")
            // Assert
            result shouldBe HttpProtocol.HTTP1
        }

        it("return null for an unknown protocol string") {
            // Arrange & Act
            val result = HttpProtocol.fromValue("HTTP/9.9")
            // Assert
            result shouldBe null
        }
    }
})
