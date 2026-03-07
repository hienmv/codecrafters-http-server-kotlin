package domain.httpResponse

enum class HttpStatus(
    val code: Int,
    val message: String,
) {
    OK_200(code = 200, message = "OK"),
    CREATED_201(code = 201, message = "Created"),

    BAD_REQUEST_400(code = 400, message = "Bad Request"),
    NOT_FOUND_404(code = 404, message = "Not Found"),
    PAYLOAD_TOO_LARGE_413(code = 413, message = "Payload Too Large"),

    SERVER_ERROR_500(code = 500, message = "Internal Server Error"),
    SERVICE_UNAVAILABLE_503(code = 503, message = "Service Unavailable"),
}
