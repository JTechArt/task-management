package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateRuleRequest
import com.aitask.core.domain.model.UpdateRuleRequest

class RuleValidator {

    fun validate(request: CreateRuleRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate name
        if (request.name.isBlank()) {
            errors.add(ValidationError("name", "Rule name cannot be blank", "REQUIRED"))
        }
        if (request.name.length > 200) {
            errors.add(ValidationError("name", "Rule name cannot exceed 200 characters", "MAX_LENGTH"))
        }

        // Validate content
        if (request.content.isBlank()) {
            errors.add(ValidationError("content", "Rule content cannot be blank", "REQUIRED"))
        }
        if (request.content.length > 100_000) {
            errors.add(ValidationError("content", "Rule content cannot exceed 100,000 characters", "MAX_LENGTH"))
        }

        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }

    fun validate(request: UpdateRuleRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate name if provided
        request.name?.let { name ->
            if (name.isBlank()) {
                errors.add(ValidationError("name", "Rule name cannot be blank", "REQUIRED"))
            }
            if (name.length > 200) {
                errors.add(ValidationError("name", "Rule name cannot exceed 200 characters", "MAX_LENGTH"))
            }
        }

        // Validate content if provided
        request.content?.let { content ->
            if (content.isBlank()) {
                errors.add(ValidationError("content", "Rule content cannot be blank", "REQUIRED"))
            }
            if (content.length > 100_000) {
                errors.add(ValidationError("content", "Rule content cannot exceed 100,000 characters", "MAX_LENGTH"))
            }
        }

        // At least one field must be provided for update
        if (request.name == null && request.content == null &&
            request.category == null && request.scope == null && request.targetIDE == null) {
            errors.add(ValidationError("update", "At least one field must be provided for update", "REQUIRED"))
        }

        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
}

