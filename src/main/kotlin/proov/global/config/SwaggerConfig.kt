package proov.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod")
class SwaggerConfig {

    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Proov API")
                    .description("Proov 서버 API 문서입니다.")
                    .version("v1"),
            )
            .components(Components().addSecuritySchemes(BEARER, bearerScheme()))
            .addSecurityItem(SecurityRequirement().addList(BEARER))

    private fun bearerScheme(): SecurityScheme =
        SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")

    companion object {
        private const val BEARER = "bearer"
    }
}
