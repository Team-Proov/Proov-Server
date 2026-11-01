package proov.global.auth.jwt.components

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import proov.global.auth.jwt.config.JwtProperties
import proov.global.auth.jwt.dto.IssuedToken
import java.time.Instant

@Component
class AccessTokenGenerator(
    private val jwtEncoder: JwtEncoder,
    private val properties: JwtProperties,
) {
    fun generate(userId: String): IssuedToken {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(properties.accessTokenTtl)
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(userId)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return IssuedToken(jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue, expiresAt)
    }
}
