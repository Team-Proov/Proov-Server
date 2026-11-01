package proov.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import proov.global.auth.jwt.handler.JwtAccessDeniedHandler
import proov.global.auth.jwt.handler.JwtAuthenticationEntryPoint

@Configuration
class SecurityConfig(
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    private val jwtAccessDeniedHandler: JwtAccessDeniedHandler,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                PERMIT_ALL.forEach { authorize(it, permitAll) }
                authorize(HttpMethod.POST, "/api/v1/auth/login", permitAll)
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt { }
                authenticationEntryPoint = jwtAuthenticationEntryPoint
            }
            exceptionHandling {
                authenticationEntryPoint = jwtAuthenticationEntryPoint
                accessDeniedHandler = jwtAccessDeniedHandler
            }
        }
        return http.build()
    }

    companion object {
        private val PERMIT_ALL = arrayOf(
            // Swagger UI
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",

            // Actuator
            "/actuator/health",

            // Error
            "/error",
        )
    }
}
