package proov.global.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.ErrorResponse as SpringErrorResponse
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 도메인에서 던진 커스텀 예외
     */
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> = respond(e, e.errorCode)

    /**
     * @RequestBody 검증 실패 (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        respond(
            e,
            ErrorCode.INVALID_INPUT,
            e.bindingResult.allErrors.associate { error ->
                ((error as? FieldError)?.field ?: error.objectName) to error.defaultMessage.orEmpty()
            },
        )

    /**
     * @RequestParam, @PathVariable 검증 실패 (400)
     */
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse> =
        respond(
            e,
            ErrorCode.INVALID_INPUT,
            e.parameterValidationResults.associate { result ->
                result.methodParameter.parameterName.orEmpty() to result.resolvableErrors.joinToString { it.defaultMessage.orEmpty() }
            },
        )

    /**
     * 요청 본문을 읽을 수 없음 (400)
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        respond(e, ErrorCode.INVALID_INPUT)

    /**
     * 요청 파라미터 타입 변환 실패 (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        respond(e, ErrorCode.INVALID_INPUT, mapOf(e.name to "올바른 형식이 아닙니다."))

    /**
     * 필수 요청 파라미터 누락 (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> =
        respond(e, ErrorCode.INVALID_INPUT, mapOf(e.parameterName to "필수 값입니다."))

    // Accept 헤더가 JSON을 거부한 경우에는 오류 본문도 JSON으로 쓸 수 없다.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleNotAcceptable(e: HttpMediaTypeNotAcceptableException): ResponseEntity<Void> {
        log.warn("[{}] {}", ErrorCode.NOT_ACCEPTABLE.code, e.javaClass.simpleName)
        return ResponseEntity.status(e.statusCode).headers(e.headers).build()
    }

    /**
     * Spring MVC 예외의 상태 코드와 응답 헤더를 유지하고, 나머지는 500으로 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        val frameworkError = e as? SpringErrorResponse
        return if (frameworkError != null) {
            respond(
                e,
                ErrorCode.from(frameworkError.statusCode),
                headers = frameworkError.headers,
            )
        } else {
            respond(e, ErrorCode.INTERNAL_SERVER_ERROR)
        }
    }

    private fun respond(
        e: Exception,
        errorCode: BaseErrorCode,
        errors: Map<String, String> = emptyMap(),
        headers: HttpHeaders = HttpHeaders(),
    ): ResponseEntity<ErrorResponse> {
        when {
            errorCode.status.is5xxServerError -> log.error("[{}] {}", errorCode.code, e.message, e)
            e is CustomException -> log.warn("[{}] {}", errorCode.code, e.message)
            else -> log.warn("[{}] {} {}", errorCode.code, e.javaClass.simpleName, errors.keys)
        }
        return ResponseEntity.status(errorCode.status).headers(headers).body(ErrorResponse.of(errorCode, errors))
    }
}
