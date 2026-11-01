package proov.domain.auth.dto.response

import proov.global.auth.jwt.dto.IssuedToken
import java.time.Instant

data class TokenResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
) {
    companion object {
        fun from(accessToken: IssuedToken): TokenResponse = TokenResponse(accessToken.value, accessToken.expiresAt)
    }
}
