package domain.exception

class PayloadTooLargeException(
    maxSizeByte: Int,
) : RuntimeException("The request payload is too large. Maximum allowed size is $maxSizeByte bytes")
