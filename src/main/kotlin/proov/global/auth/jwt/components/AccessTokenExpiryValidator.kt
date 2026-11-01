package proov.global.auth.jwt.components

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import java.time.Instant

/**
 * 만료된 토큰을 다른 검증 실패와 구분해 클라이언트가 재발급을 요청할 수 있게 한다.
 */
class AccessTokenExpiryValidator : OAuth2TokenValidator<Jwt> {
    private val timestampValidator = JwtTimestampValidator().apply { setAllowEmptyExpiryClaim(false) }

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val result = timestampValidator.validate(token)
        val expiresAt = token.expiresAt
        if (!result.hasErrors() || expiresAt == null || Instant.now().isBefore(expiresAt)) {
            return result
        }
        return OAuth2TokenValidatorResult.failure(OAuth2Error(EXPIRED_TOKEN, "만료된 토큰", null))
    }

    companion object {
        const val EXPIRED_TOKEN = "expired_token"
    }
}
