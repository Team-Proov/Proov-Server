package proov.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf(REQUEST_ID_PATTERN::matches) ?: UUID.randomUUID().toString()
        MDC.put(REQUEST_ID_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            if (!isSilent(request)) {
                val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
                log.info("{} {} {} {}ms", request.method, request.requestURI, response.status, elapsedMillis)
            }
            MDC.remove(REQUEST_ID_KEY)
        }
    }

    private fun isSilent(request: HttpServletRequest): Boolean =
        SILENT_PATH_PREFIXES.any { request.requestURI.startsWith(it) }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val REQUEST_ID_KEY = "requestId"
        private const val NANOS_PER_MILLI = 1_000_000L
        private val REQUEST_ID_PATTERN = Regex("^[A-Za-z0-9-]{1,64}$")
        private val SILENT_PATH_PREFIXES = listOf("/actuator", "/swagger-ui", "/v3/api-docs")
    }
}
