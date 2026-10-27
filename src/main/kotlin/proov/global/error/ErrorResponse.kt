package proov.global.error

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
class ErrorResponse private constructor(
    val status: Int,
    val code: String,
    val message: String,
    val errors: List<FieldError>,
) {
    class FieldError(
        val field: String,
        val reason: String,
    )

    companion object {
        fun of(errorCode: ErrorCode, errors: List<FieldError> = emptyList()) =
            ErrorResponse(errorCode.status.value(), errorCode.code, errorCode.message, errors)
    }
}
