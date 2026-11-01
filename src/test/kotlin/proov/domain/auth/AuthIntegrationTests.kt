package proov.domain.auth

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import proov.TestcontainersConfiguration
import proov.domain.auth.social.SocialTokenVerifier
import proov.domain.user.enums.SocialProvider
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AuthIntegrationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val jwtEncoder: JwtEncoder,
    @Autowired private val jsonMapper: JsonMapper,
) {

    @MockitoBean
    private lateinit var socialTokenVerifier: SocialTokenVerifier

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("delete from users")
        jdbcTemplate.update(
            "insert into users (id, social_provider, social_id, created_at, updated_at) values (?, ?, ?, now(), now())",
            USER_ID,
            SocialProvider.KAKAO.name,
            SOCIAL_ID,
        )
        given(socialTokenVerifier.verify(SocialProvider.KAKAO, REGISTERED_ID_TOKEN)).willReturn(SOCIAL_ID)
        given(socialTokenVerifier.verify(SocialProvider.KAKAO, UNREGISTERED_ID_TOKEN)).willReturn("unknown")
        given(socialTokenVerifier.verify(SocialProvider.KAKAO, FORGED_ID_TOKEN))
            .willThrow(CustomException(ErrorCode.INVALID_ID_TOKEN))
    }

    @Test
    fun loginIssuesAccessTokenForRegisteredUser() {
        mockMvc.perform(login("KAKAO", REGISTERED_ID_TOKEN))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.accessTokenExpiresAt").isNotEmpty)
    }

    @Test
    fun loginSucceedsEvenWithStaleBearerHeader() {
        mockMvc.perform(login("KAKAO", REGISTERED_ID_TOKEN).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
            .andExpect(status().isOk)
    }

    @Test
    fun loginRequiresSignupForUnknownSocialAccount() {
        mockMvc.perform(login("KAKAO", UNREGISTERED_ID_TOKEN))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("SIGNUP_REQUIRED"))
    }

    @Test
    fun loginRejectsInvalidIdToken() {
        mockMvc.perform(login("KAKAO", FORGED_ID_TOKEN))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_ID_TOKEN"))
    }

    @Test
    fun loginRejectsUnsupportedProvider() {
        mockMvc.perform(login("NAVER", REGISTERED_ID_TOKEN))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.errors.provider").exists())
    }

    @Test
    fun accessTokenAuthenticatesRequest() {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer ${loginAndGetAccessToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(USER_ID))
    }

    @Test
    fun missingTokenIsUnauthorized() {
        mockMvc.perform(get(PROTECTED_PATH))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun malformedTokenIsInvalid() {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
    }

    @Test
    fun expiredTokenIsDistinguished() {
        val issuedAt = Instant.now().minus(Duration.ofHours(2))
        val claims = JwtClaimsSet.builder()
            .issuer("proov")
            .subject(USER_ID)
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(Duration.ofMinutes(30)))
            .build()
        val expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue

        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer $expired"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("EXPIRED_TOKEN"))
    }

    @Test
    fun accessTokenWithoutExpirationIsInvalid() {
        val claims = JwtClaimsSet.builder()
            .issuer("proov")
            .subject(USER_ID)
            .issuedAt(Instant.now())
            .build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue

        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
    }

    private fun login(provider: String, idToken: String) =
        post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"provider":"$provider","idToken":"$idToken"}""")

    private fun loginAndGetAccessToken(): String {
        val body = mockMvc.perform(login("KAKAO", REGISTERED_ID_TOKEN)).andReturn().response.contentAsString
        return jsonMapper.readTree(body).path("data").path("accessToken").asString()
    }

    companion object {
        private const val USER_ID = "0ABCDEFGHJKMN"
        private const val SOCIAL_ID = "kakao-user"
        private const val REGISTERED_ID_TOKEN = "registered-id-token"
        private const val UNREGISTERED_ID_TOKEN = "unregistered-id-token"
        private const val FORGED_ID_TOKEN = "forged-id-token"
        private const val PROTECTED_PATH = "/test/me"
    }
}

@RestController
class AuthenticatedProbeController {
    @GetMapping("/test/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): Map<String, String> = mapOf("userId" to jwt.subject.orEmpty())
}
