package proov.domain.auth.service

import org.springframework.stereotype.Service
import proov.domain.auth.dto.command.LoginCommand
import proov.domain.auth.dto.response.TokenResponse
import proov.domain.auth.social.SocialTokenVerifier
import proov.domain.user.repository.UserRepository
import proov.global.auth.jwt.components.AccessTokenGenerator
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode

@Service
class AuthService(
    private val socialTokenVerifier: SocialTokenVerifier,
    private val userRepository: UserRepository,
    private val accessTokenGenerator: AccessTokenGenerator,
) {
    fun login(command: LoginCommand): TokenResponse {
        val socialId = socialTokenVerifier.verify(command.provider, command.idToken)
        val user = userRepository.findBySocialProviderAndSocialId(command.provider, socialId)
            ?: throw CustomException(ErrorCode.SIGNUP_REQUIRED)
        return TokenResponse.from(accessTokenGenerator.generate(user.id))
    }
}
