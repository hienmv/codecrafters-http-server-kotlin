package common

enum class HttpContentType(val value: String) {
    JSON("application/json"),
    TEXT("text/plain"),
    HTML("text/html"),
    OCTET_STREAM("application/octet-stream"),
}