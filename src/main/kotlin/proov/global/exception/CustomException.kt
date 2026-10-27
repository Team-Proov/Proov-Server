package proov.global.exception

class CustomException(
    val errorCode: BaseErrorCode,
    detail: String? = null,
    cause: Throwable? = null,
) : RuntimeException(detail ?: errorCode.message, cause)
