package proov.domain.auth.social

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "proov.oauth")
data class OAuthProperties(
    val apple: Provider = Provider(),
    val kakao: Provider = Provider(),
    val google: Provider = Provider(),
) {
    data class Provider(
        val clientIds: List<String> = emptyList(),
    )
}
