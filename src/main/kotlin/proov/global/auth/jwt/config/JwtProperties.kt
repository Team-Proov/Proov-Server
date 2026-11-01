package proov.global.auth.jwt.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "proov.jwt")
data class JwtProperties(
    @field:NotBlank
    val issuer: String,
    @field:Size(min = 32)
    val secret: String,
    val accessTokenTtl: Duration,
)
