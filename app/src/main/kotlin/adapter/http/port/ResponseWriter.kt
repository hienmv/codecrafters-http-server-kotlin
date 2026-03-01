package adapter.http.port

import domain.httpResponse.HttpResponse

interface ResponseWriter {
    fun writeResponse(response: HttpResponse)
}
