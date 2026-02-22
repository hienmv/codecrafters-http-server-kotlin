package adapters

import domain.httpResponse.HttpResponse

interface ResponseWriter {
    fun writeResponse(response: HttpResponse)
}