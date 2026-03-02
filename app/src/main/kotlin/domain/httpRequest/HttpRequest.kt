package domain.httpRequest

import domain.vo.HttpProtocol

data class HttpRequest(
    val method: HttpMethod,
    val target: String,
    val protocol: HttpProtocol,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpRequest) return false

        return method == other.method &&
            target == other.target &&
            protocol == other.protocol &&
            headers == other.headers &&
            body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + protocol.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }

    override fun toString(): String =
        "HttpRequest(method=$method, target='$target', protocol=$protocol, headers=$headers, body=${body.contentToString()})"
}
