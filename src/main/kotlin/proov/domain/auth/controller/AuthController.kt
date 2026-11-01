package proov.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import proov.domain.auth.dto.request.LoginRequest
import proov.domain.auth.dto.response.TokenResponse
import proov.domain.auth.service.AuthService
import proov.global.common.response.ApiResponse

@Tag(name = "Auth", description = "소셜 로그인")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(
        summary = "소셜 로그인",
        description = "애플, 카카오, 구글 ID 토큰으로 로그인합니다. 가입되지 않은 계정이면 404 SIGNUP_REQUIRED를 반환합니다.",
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<ApiResponse<TokenResponse>> = ApiResponse.ok(authService.login(request.toCommand()))
}
