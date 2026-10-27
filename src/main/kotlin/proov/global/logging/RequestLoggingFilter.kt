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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        MDC.get(TRACE_ID_KEY)?.let { response.setHeader(TRACE_ID_HEADER, it) }
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            if (!isSilent(request)) {
                val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
                log.info("{} {} {} {}ms", request.method, request.requestURI, response.status, elapsedMillis)
            }
        }
    }

    private fun isSilent(request: HttpServletRequest): Boolean =
        SILENT_PATH_PREFIXES.any { request.requestURI.startsWith(it) }

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        private const val TRACE_ID_KEY = "traceId"
        private const val NANOS_PER_MILLI = 1_000_000L
        private val SILENT_PATH_PREFIXES = listOf("/actuator", "/swagger-ui", "/v3/api-docs")
    }
}
