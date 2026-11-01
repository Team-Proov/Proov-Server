package proov.domain.auth.social

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import proov.domain.user.enums.SocialProvider
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode
import java.time.Duration
import java.time.Instant
import java.util.Date

class SocialTokenVerifierTests {

    private val key: RSAKey = RSAKeyGenerator(2048).keyID("test-key").generate()
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val verifier = SocialTokenVerifier(
        OAuthProperties(kakao = OAuthProperties.Provider(listOf(CLIENT_ID))),
        restTemplate,
    )

    @Test
    fun returnsSubjectOfValidIdToken() {
        respondWithKeys()

        assertThat(verifier.verify(SocialProvider.KAKAO, idToken())).isEqualTo("kakao-user")
    }

    @Test
    fun rejectsTokenForAnotherAudience() {
        respondWithKeys()

        assertErrorCode(idToken(audience = "other-app"), ErrorCode.INVALID_ID_TOKEN)
    }

    @Test
    fun rejectsTokenFromAnotherIssuer() {
        respondWithKeys()

        assertErrorCode(idToken(issuer = "https://accounts.google.com"), ErrorCode.INVALID_ID_TOKEN)
    }

    @Test
    fun rejectsExpiredToken() {
        respondWithKeys()

        assertErrorCode(idToken(expiresAt = Instant.now().minus(Duration.ofHours(1))), ErrorCode.INVALID_ID_TOKEN)
    }

    @Test
    fun rejectsTokenSignedByUnknownKey() {
        respondWithKeys()
        val otherKey = RSAKeyGenerator(2048).keyID("test-key").generate()

        assertErrorCode(idToken(signingKey = otherKey), ErrorCode.INVALID_ID_TOKEN)
    }

    @Test
    fun reportsUnavailableWhenKeysCannotBeFetched() {
        server.expect(ExpectedCount.manyTimes(), requestTo(KAKAO_JWKS)).andRespond(withServerError())

        assertErrorCode(idToken(), ErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
    }

    private fun respondWithKeys() {
        server.expect(ExpectedCount.manyTimes(), requestTo(KAKAO_JWKS))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(JWKSet(key.toPublicJWK()).toString(), MediaType.APPLICATION_JSON))
    }

    private fun assertErrorCode(idToken: String, errorCode: ErrorCode) {
        assertThatThrownBy { verifier.verify(SocialProvider.KAKAO, idToken) }
            .isInstanceOfSatisfying(CustomException::class.java) { assertThat(it.errorCode).isEqualTo(errorCode) }
    }

    private fun idToken(
        issuer: String = "https://kauth.kakao.com",
        audience: String = CLIENT_ID,
        expiresAt: Instant = Instant.now().plus(Duration.ofHours(1)),
        signingKey: RSAKey = key,
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject("kakao-user")
            .issueTime(Date.from(expiresAt.minus(Duration.ofHours(2))))
            .expirationTime(Date.from(expiresAt))
            .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.keyID).build(), claims)
            .apply { sign(RSASSASigner(signingKey)) }
            .serialize()
    }

    companion object {
        private const val CLIENT_ID = "kakao-app-key"
        private const val KAKAO_JWKS = "https://kauth.kakao.com/.well-known/jwks.json"
    }
}
