package proov.global.common.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EnumValidator : ConstraintValidator<ValidEnum, String> {
    private lateinit var allowed: Set<String>
    private var ignoreCase: Boolean = true

    override fun initialize(annotation: ValidEnum) {
        ignoreCase = annotation.ignoreCase
        allowed = annotation.enumClass.java.enumConstants.mapTo(HashSet()) { normalize(it.name) }
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean =
        value == null || normalize(value) in allowed

    private fun normalize(value: String): String = if (ignoreCase) value.uppercase() else value
}
