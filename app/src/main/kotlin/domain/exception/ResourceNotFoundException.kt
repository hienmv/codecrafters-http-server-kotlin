package domain.exception

class ResourceNotFoundException(
    resource: String,
) : RuntimeException("Resource not found: $resource")
