package com.aitask.core.domain.validation

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList()
) {
    companion object {
        fun success() = ValidationResult(isValid = true, errors = emptyList())
        fun failure(vararg errors: ValidationError) = ValidationResult(isValid = false, errors = errors.toList())
        fun failure(errors: List<ValidationError>) = ValidationResult(isValid = false, errors = errors)
    }
}

data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)

interface Validator<T> {
    fun validate(input: T): ValidationResult
}

