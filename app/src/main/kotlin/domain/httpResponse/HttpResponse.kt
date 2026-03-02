package domain.httpResponse

import domain.vo.HttpProtocol

data class HttpResponse(
    val status: HttpStatus,
    val protocol: HttpProtocol = HttpProtocol.HTTP11,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpResponse) return false

        return status == other.status &&
            protocol == other.protocol &&
            headers == other.headers &&
            (body?.contentEquals(other.body ?: return false) ?: (other.body == null))
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }

    override fun toString(): String = "HttpResponse(status=$status, protocol=$protocol, headers=$headers, body=${body?.contentToString()})"
}
