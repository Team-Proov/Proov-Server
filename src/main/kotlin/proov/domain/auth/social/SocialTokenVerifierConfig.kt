package proov.domain.auth.social

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
@EnableConfigurationProperties(OAuthProperties::class)
class SocialTokenVerifierConfig {

    @Bean
    fun socialTokenVerifier(properties: OAuthProperties): SocialTokenVerifier =
        SocialTokenVerifier(
            properties,
            RestTemplate(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(CONNECT_TIMEOUT)
                    setReadTimeout(READ_TIMEOUT)
                },
            ),
        )

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(1)
        private val READ_TIMEOUT = Duration.ofSeconds(3)
    }
}
