package proov.global.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ApiResponse<T> private constructor(
    val status: Int,
    val code: String,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> ok(data: T): ResponseEntity<ApiResponse<T>> = of(HttpStatus.OK, "요청에 성공했습니다.", data)

        fun <T> created(data: T): ResponseEntity<ApiResponse<T>> = of(HttpStatus.CREATED, "생성되었습니다.", data)

        private fun <T> of(status: HttpStatus, message: String, data: T): ResponseEntity<ApiResponse<T>> =
            ResponseEntity.status(status).body(ApiResponse(status.value(), status.name, message, data))
    }
}
