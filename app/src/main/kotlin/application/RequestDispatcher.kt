package application

import domain.httpRequest.HttpMethod
import domain.httpRequest.HttpRequest
import domain.httpResponse.HttpResponse
import domain.httpResponse.HttpStatus

class RequestDispatcher(private val fileRepository: FileRepository) {
    fun dispatch(request: HttpRequest): HttpResponse {
        val ctx = HttpContext(request)
        return when {
            request.target == "/" -> ctx.resultOK()
            request.target.startsWith("/echo/") -> {
                ctx.resultText(request.target.removePrefix("/echo/"))
            }

            request.target == "/user-agent" -> {
                ctx.resultText(request.headers["User-Agent"] ?: "Unknown")
            }

            request.target.startsWith("/files/") -> when (request.method) {
                HttpMethod.GET -> {
                    // TODO: not for big files, we should stream the file content to the response
                    //  instead of loading it all in memory
                    val fileName = request.target.removePrefix("/files/")
                    val content = fileRepository.read(fileName)
                    if (content == null) {
                        ctx.resultError(HttpStatus.NOT_FOUND_404)
                    } else {
                        ctx.resultBytes(content)
                    }
                }

                HttpMethod.POST -> {
                    val fileName = request.target.removePrefix("/files/")
                    // TODO: not for big files, we should stream the request body to the file
                    //  instead of loading it all in memory
                    if (fileRepository.write(fileName, request.body)) {
                        ctx.resultOK(HttpStatus.CREATED_201)
                    } else {
                        ctx.resultError(HttpStatus.BAD_REQUEST_400)
                    }
                }

                else -> ctx.resultError(HttpStatus.BAD_REQUEST_400)
            }

            else -> ctx.resultError(HttpStatus.NOT_FOUND_404)
        }
    }
}