package proov

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProovServerApplication

fun main(args: Array<String>) {
    runApplication<ProovServerApplication>(*args)
}
