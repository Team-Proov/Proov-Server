package proov.global.error

abstract class BusinessException(
    val errorCode: ErrorCode,
    detail: String? = null,
    cause: Throwable? = null,
) : RuntimeException(detail ?: errorCode.message, cause)
