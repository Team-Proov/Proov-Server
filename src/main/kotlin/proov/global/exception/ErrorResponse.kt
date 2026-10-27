package proov.global.exception

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ErrorResponse private constructor(
    val status: Int,
    val code: String,
    val message: String,
    val errors: Map<String, String>,
) {
    companion object {
        fun of(
            errorCode: BaseErrorCode,
            errors: Map<String, String> = emptyMap(),
        ) = ErrorResponse(errorCode.status.value(), errorCode.code, errorCode.message, errors)
    }
}
