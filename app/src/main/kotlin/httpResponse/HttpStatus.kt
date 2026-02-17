package httpResponse

enum class HttpStatus(val code: Int, val message: String) {
    OK_200(code = 200, message = "OK"),
    CREATED(code = 201, message = "Created"),

    BAD_REQUEST(code = 400, message = "Bad Request"),
    NOTFOUND_404(code = 404, message = "Not Found")
}