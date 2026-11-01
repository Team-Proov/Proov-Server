package proov.global.auth.jwt.dto

import java.time.Instant

data class IssuedToken(
    val value: String,
    val expiresAt: Instant,
)
