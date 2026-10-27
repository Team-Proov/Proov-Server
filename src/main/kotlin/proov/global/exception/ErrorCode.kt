package proov.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode

enum class ErrorCode(
    override val status: HttpStatus,
    override val message: String,
) : BaseErrorCode {
    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 Not Found
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

    // 406 Not Acceptable
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다."),

    // 413 Payload Too Large
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "요청 크기가 너무 큽니다."),

    // 415 Unsupported Media Type
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 503 Service Unavailable
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "잠시 후 다시 시도해 주세요."),
    ;

    override val code: String get() = name

    companion object {
        fun from(status: HttpStatusCode): BaseErrorCode =
            entries.firstOrNull { it.status.value() == status.value() }
                ?: object : BaseErrorCode {
                    override val status = status
                    override val code = "HTTP_${status.value()}"
                    override val message =
                        if (status.is4xxClientError) "요청을 처리할 수 없습니다." else INTERNAL_SERVER_ERROR.message
                }
    }
}
