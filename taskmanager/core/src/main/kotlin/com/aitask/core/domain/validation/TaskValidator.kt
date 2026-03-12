package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateTaskRequest

class TaskValidator : Validator<CreateTaskRequest> {
    override fun validate(input: CreateTaskRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate title
        if (input.title.isBlank()) {
            errors.add(ValidationError("title", "Task title is required", "REQUIRED"))
        } else if (input.title.length > 500) {
            errors.add(ValidationError("title", "Task title must not exceed 500 characters", "MAX_LENGTH"))
        }
        
        // Validate description
        input.description?.let { desc ->
            if (desc.length > 50000) {
                errors.add(ValidationError("description", "Description must not exceed 50000 characters", "MAX_LENGTH"))
            }
        }
        
        // Validate workspace path
        input.workspacePath?.let { path ->
            if (path.length > 1000) {
                errors.add(ValidationError("workspacePath", "Workspace path must not exceed 1000 characters", "MAX_LENGTH"))
            }
        }
        
        // Validate branch name
        input.branchName?.let { branch ->
            if (branch.length > 200) {
                errors.add(ValidationError("branchName", "Branch name must not exceed 200 characters", "MAX_LENGTH"))
            }
            if (!branch.matches(Regex("^[a-zA-Z0-9/_\\-]+$"))) {
                errors.add(ValidationError("branchName", "Branch name contains invalid characters", "INVALID_FORMAT"))
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
}

