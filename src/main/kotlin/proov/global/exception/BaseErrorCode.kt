package proov.global.exception

import org.springframework.http.HttpStatusCode

interface BaseErrorCode {
    val status: HttpStatusCode
    val code: String
    val message: String
}
