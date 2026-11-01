package proov.global.common.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class EnumValidator : ConstraintValidator<ValidEnum, String> {
    private lateinit var names: Set<String>

    override fun initialize(annotation: ValidEnum) {
        names = annotation.enumClass.java.enumConstants.mapTo(HashSet()) { it.name }
    }

    override fun isValid(
        value: String?,
        context: ConstraintValidatorContext,
    ): Boolean = value == null || value in names
}
