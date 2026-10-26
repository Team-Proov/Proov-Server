package proov.global.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [EnumValidator::class])
annotation class ValidEnum(
    val enumClass: KClass<out Enum<*>>,
    val ignoreCase: Boolean = true,
    val message: String = "허용되지 않는 값입니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
