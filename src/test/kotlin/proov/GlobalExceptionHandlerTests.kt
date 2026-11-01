package proov

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import proov.global.exception.CustomException
import proov.global.exception.ErrorCode

@WebMvcTest(controllers = [ExceptionProbeController::class])
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTests(@Autowired private val mockMvc: MockMvc) {

    @Test
    fun preservesMethodNotAllowedStatusAndAllowHeader() {
        mockMvc.perform(post("/exception-probe"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(header().string("Allow", containsString("GET")))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(jsonPath("$.status").value(405))
    }

    @Test
    fun preservesNotAcceptableStatus() {
        mockMvc.perform(get("/exception-probe").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isNotAcceptable)
    }

    @Test
    fun preservesUnsupportedMediaTypeStatus() {
        mockMvc.perform(post("/exception-probe-json").contentType(MediaType.TEXT_PLAIN).content("text"))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(jsonPath("$.status").value(415))
    }

    @Test
    fun handlesMalformedJsonAsBadRequest() {
        mockMvc.perform(post("/exception-probe-json").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    fun handlesUnknownPathAsNotFound() {
        mockMvc.perform(get("/no-such-path"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun supportsDirectCustomException() {
        mockMvc.perform(get("/exception-probe-custom"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CONFLICT"))
            .andExpect(jsonPath("$.status").value(409))
    }
}

@RestController
class ExceptionProbeController {
    @GetMapping("/exception-probe", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun get(): Map<String, String> = mapOf("result" to "ok")

    @PostMapping("/exception-probe-json", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun post(@RequestBody body: Map<String, String>): Map<String, String> = body

    @GetMapping("/exception-probe-custom")
    fun custom(): Nothing = throw CustomException(ErrorCode.CONFLICT)
}
