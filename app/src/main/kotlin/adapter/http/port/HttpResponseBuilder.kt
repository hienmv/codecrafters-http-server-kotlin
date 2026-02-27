package adapter.http.port

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

interface HttpResponseBuilder {
    fun result(status: HttpStatus): HttpResponse
    fun resultText(text: String): HttpResponse
    fun resultBytes(content: ByteArray): HttpResponse
}