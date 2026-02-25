package adapter.http.port

import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

interface HttpResponseBuilder {
    fun resultOK(): HttpResponse
    fun resultCreated(): HttpResponse
    fun resultText(text: String): HttpResponse
    fun resultBytes(content: ByteArray): HttpResponse
    fun resultError(status: HttpStatus): HttpResponse
}