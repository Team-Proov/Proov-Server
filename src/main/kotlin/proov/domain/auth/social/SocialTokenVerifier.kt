package proov.domain.auth.social

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.web.client.RestOperations
import proov.domain.user.enums.SocialProvider
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode
import java.time.Instant

/**
 * 애플, 카카오, 구글의 OIDC ID 토큰을 각 제공자의 공개키(JWKS)로 검증한다.
 */
class SocialTokenVerifier(
    properties: OAuthProperties,
    restOperations: RestOperations,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val decoders: Map<SocialProvider, JwtDecoder> =
        OidcProvider.entries.associate { it.socialProvider to decoder(it, it.clientIds(properties), restOperations) }

    /**
     * 검증에 성공하면 소셜 계정 식별자(sub)를 반환한다.
     */
    fun verify(provider: SocialProvider, idToken: String): String {
        val jwt = try {
            decoders.getValue(provider).decode(idToken)
        } catch (e: BadJwtException) {
            throw CustomException(ErrorCode.INVALID_ID_TOKEN, "${provider.name} ${e.message}", e)
        } catch (e: JwtException) {
            throw CustomException(ErrorCode.SOCIAL_LOGIN_UNAVAILABLE, "${provider.name} ${e.message}", e)
        }
        return jwt.subject?.takeIf { it.isNotBlank() }
            ?: throw CustomException(ErrorCode.INVALID_ID_TOKEN, "${provider.name} sub 없음")
    }

    private fun decoder(
        provider: OidcProvider,
        clientIds: List<String>,
        restOperations: RestOperations,
    ): JwtDecoder {
        if (clientIds.isEmpty()) {
            log.warn("{} client ID가 설정되지 않아 이 제공자의 ID 토큰은 모두 거절합니다.", provider.name)
        }
        val claimConverter = MappedJwtClaimSetConverter.withDefaults(emptyMap())
        return NimbusJwtDecoder.withJwkSetUri(provider.jwkSetUri)
            .restOperations(restOperations)
            .build()
            .apply {
                setClaimSetConverter { claims ->
                    claimConverter.convert(claims).apply {
                        // Spring Security가 누락된 iat를 exp - 1초로 채우므로 원본 클레임을 확인한다.
                        if (JwtClaimNames.IAT !in claims) remove(JwtClaimNames.IAT)
                    }
                }
                setJwtValidator(
                    DelegatingOAuth2TokenValidator(
                        JwtTimestampValidator().apply { setAllowEmptyExpiryClaim(false) },
                        JwtClaimValidator<Instant>(JwtClaimNames.IAT) { true },
                        JwtClaimValidator<String>(JwtClaimNames.ISS) { it in provider.issuers },
                        JwtClaimValidator<List<String>>(JwtClaimNames.AUD) { audiences -> audiences.any { it in clientIds } },
                    ),
                )
            }
    }
}
