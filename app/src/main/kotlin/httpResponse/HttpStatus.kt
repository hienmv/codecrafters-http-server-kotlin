package httpResponse

enum class HttpStatus(val code: Int, val message: String) {
    OK_200(code = 200, message = "OK"),
    CREATED_201(code = 201, message = "Created"),

    BAD_REQUEST_400(code = 400, message = "Bad Request"),
    NOT_FOUND_404(code = 404, message = "Not Found")
}