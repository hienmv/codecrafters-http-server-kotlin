package domain.exception

class WriteFailedException(
    fileName: String,
) : RuntimeException("Failed to write content to file: $fileName")
