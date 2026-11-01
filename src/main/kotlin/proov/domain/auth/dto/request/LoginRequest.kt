package proov.domain.auth.dto.request

import jakarta.validation.constraints.NotBlank
import proov.domain.auth.dto.command.LoginCommand
import proov.domain.user.enums.SocialProvider
import proov.global.common.validation.ValidEnum

data class LoginRequest(
    @field:NotBlank
    @field:ValidEnum(SocialProvider::class)
    val provider: String?,
    @field:NotBlank
    val idToken: String?,
) {
    fun toCommand(): LoginCommand = LoginCommand(SocialProvider.valueOf(provider!!), idToken!!)
}
