package proov.global.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import proov.global.response.ErrorResponse

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logByStatus(e.errorCode, e)
        return respond(e.errorCode)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(e: Exception): ResponseEntity<ErrorResponse> {
        logByStatus(GlobalErrorCode.INTERNAL_SERVER_ERROR, e)
        return respond(GlobalErrorCode.INTERNAL_SERVER_ERROR)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors.map { ErrorResponse.FieldError(it.field, it.defaultMessage.orEmpty()) }
        return invalidInput(ex, errors)
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.parameterValidationResults.flatMap { result ->
            val parameter = result.methodParameter.parameterName.orEmpty()
            result.resolvableErrors.map { ErrorResponse.FieldError(parameter, it.defaultMessage.orEmpty()) }
        }
        return invalidInput(ex, errors)
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorCode = GlobalErrorCode.from(statusCode)
        logByStatus(errorCode, ex)
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(errorCode))
    }

    private fun invalidInput(ex: Exception, errors: List<ErrorResponse.FieldError>): ResponseEntity<Any> {
        logByStatus(GlobalErrorCode.INVALID_INPUT, ex)
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT.status).body(ErrorResponse.of(GlobalErrorCode.INVALID_INPUT, errors))
    }

    private fun respond(errorCode: ErrorCode): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(errorCode.status).body(ErrorResponse.of(errorCode))

    private fun logByStatus(errorCode: ErrorCode, e: Exception) {
        if (errorCode.status.is5xxServerError) {
            log.error("[{}] {}", errorCode.code, e.message, e)
        } else {
            log.warn("[{}] {}", errorCode.code, e.message)
        }
    }
}
