package infrastructure.http.error

import adapter.http.port.HttpErrorHandler
import domain.httpResponse.HttpResponse
import kotlin.reflect.KClass

class CompositeHttpErrorHandler(
    private val handlers: List<Pair<KClass<out Throwable>, HttpErrorHandler>>,
    private val fallbackHandler: HttpErrorHandler,
) : HttpErrorHandler {
    override fun handle(t: Throwable): HttpResponse =
        handlers.firstOrNull { it.first.isInstance(t) }?.second?.handle(t)
            ?: fallbackHandler.handle(t)
}
