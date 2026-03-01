package domain.httpResponse

import domain.vo.HttpProtocol

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
)
