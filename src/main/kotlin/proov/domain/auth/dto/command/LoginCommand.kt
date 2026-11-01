package proov.domain.auth.dto.command

import proov.domain.user.enums.SocialProvider

data class LoginCommand(
    val provider: SocialProvider,
    val idToken: String,
)
