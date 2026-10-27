package proov.global.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException, request: WebRequest): ResponseEntity<Any>? {
        if (e.errorCode.status.is5xxServerError) {
            log.error("[{}] {}", e.errorCode.code, e.message, e)
        } else {
            log.warn("[{}] {}", e.errorCode.code, e.message)
        }
        return respond(e, e.errorCode, emptyList(), request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(e: Exception, request: WebRequest): ResponseEntity<Any>? {
        log.error("[{}] {}", GlobalErrorCode.INTERNAL_SERVER_ERROR.code, e.message, e)
        return respond(e, GlobalErrorCode.INTERNAL_SERVER_ERROR, emptyList(), request)
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorCode = GlobalErrorCode.from(statusCode)
        val errors = fieldErrorsOf(ex)
        if (errorCode.status.is5xxServerError) {
            log.error("[{}] {}", errorCode.code, ex.message, ex)
        } else {
            log.warn("[{}] {} {}", errorCode.code, ex.javaClass.simpleName, errors.map { it.field })
        }
        return respond(ex, errorCode, errors, request, headers)
    }

    private fun respond(
        ex: Exception,
        errorCode: ErrorCode,
        errors: List<ErrorResponse.FieldError>,
        request: WebRequest,
        headers: HttpHeaders = HttpHeaders(),
    ): ResponseEntity<Any>? =
        super.handleExceptionInternal(ex, ErrorResponse.of(errorCode, errors), headers, errorCode.status, request)

    private fun fieldErrorsOf(ex: Exception): List<ErrorResponse.FieldError> = when (ex) {
        is MethodArgumentNotValidException -> ex.bindingResult.allErrors.map { error ->
            ErrorResponse.FieldError((error as? FieldError)?.field ?: error.objectName, error.defaultMessage.orEmpty())
        }
        is HandlerMethodValidationException -> ex.parameterValidationResults.flatMap { result ->
            val parameter = result.methodParameter.parameterName.orEmpty()
            result.resolvableErrors.map { ErrorResponse.FieldError(parameter, it.defaultMessage.orEmpty()) }
        }
        else -> emptyList()
    }
}
