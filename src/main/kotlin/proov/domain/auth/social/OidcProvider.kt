package proov.domain.auth.social

import proov.domain.user.enums.SocialProvider

enum class OidcProvider(
    val socialProvider: SocialProvider,
    val jwkSetUri: String,
    val issuers: Set<String>,
) {
    APPLE(
        SocialProvider.APPLE,
        "https://appleid.apple.com/auth/keys",
        setOf("https://appleid.apple.com"),
    ),
    KAKAO(
        SocialProvider.KAKAO,
        "https://kauth.kakao.com/.well-known/jwks.json",
        setOf("https://kauth.kakao.com"),
    ),
    GOOGLE(
        SocialProvider.GOOGLE,
        "https://www.googleapis.com/oauth2/v3/certs",
        setOf("https://accounts.google.com", "accounts.google.com"),
    ),
    ;

    fun clientIds(properties: OAuthProperties): List<String> =
        when (this) {
            APPLE -> properties.apple
            KAKAO -> properties.kakao
            GOOGLE -> properties.google
        }.clientIds.filter { it.isNotBlank() }
}
