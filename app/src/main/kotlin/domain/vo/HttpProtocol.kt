package domain.vo

enum class HttpProtocol(
    val value: String,
) {
    HTTP1("HTTP/1.0"),
    HTTP11("HTTP/1.1"),
    ;

    companion object {
        fun fromValue(value: String): HttpProtocol? = entries.find { it.value == value }
    }
}
