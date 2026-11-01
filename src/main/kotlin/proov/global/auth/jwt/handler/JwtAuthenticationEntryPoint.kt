package proov.global.auth.jwt.handler

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import proov.global.auth.jwt.components.AccessTokenExpiryValidator
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode

@Component
class JwtAuthenticationEntryPoint(
    @Qualifier("handlerExceptionResolver")
    private val resolver: HandlerExceptionResolver,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val errorCode = when {
            authException is InsufficientAuthenticationException -> ErrorCode.UNAUTHORIZED
            authException.isExpired() -> ErrorCode.EXPIRED_TOKEN
            else -> ErrorCode.INVALID_TOKEN
        }
        resolver.resolveException(request, response, null, CustomException(errorCode))
    }

    private fun Throwable.isExpired(): Boolean =
        generateSequence(this) { it.cause }
            .filterIsInstance<JwtValidationException>()
            .any { e -> e.errors.any { it.errorCode == AccessTokenExpiryValidator.EXPIRED_TOKEN } }
}
