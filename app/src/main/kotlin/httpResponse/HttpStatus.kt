package httpResponse

enum class HttpStatus(val code: Int, val message: String) {
    OK_200(code = 200, message = "OK"),
    NOTFOUND_404(code = 404, message = "Not Found")
}