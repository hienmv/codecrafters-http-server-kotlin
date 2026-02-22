package domain.httpRequest

import domain.vo.HttpProtocol

data class HttpRequest(
    val method: HttpMethod,
    val target: String,
    val protocol: HttpProtocol,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
)