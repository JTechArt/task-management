package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateProjectRequest
import java.nio.file.Paths

class ProjectValidator : Validator<CreateProjectRequest> {
    override fun validate(input: CreateProjectRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate name
        if (input.name.isBlank()) {
            errors.add(ValidationError("name", "Project name is required", "REQUIRED"))
        } else if (input.name.length > 200) {
            errors.add(ValidationError("name", "Project name must not exceed 200 characters", "MAX_LENGTH"))
        } else if (!input.name.matches(Regex("^[a-zA-Z0-9\\s\\-_]+$"))) {
            errors.add(ValidationError("name", "Project name contains invalid characters", "INVALID_FORMAT"))
        }
        
        // Validate workspace path
        if (input.workspacePath.isBlank()) {
            errors.add(ValidationError("workspacePath", "Workspace path is required", "REQUIRED"))
        } else if (input.workspacePath.length > 1000) {
            errors.add(ValidationError("workspacePath", "Workspace path must not exceed 1000 characters", "MAX_LENGTH"))
        } else {
            try {
                Paths.get(input.workspacePath)
            } catch (e: Exception) {
                errors.add(ValidationError("workspacePath", "Workspace path is not a valid file system path", "INVALID_PATH"))
            }
        }
        
        // Validate branch template
        if (input.branchTemplate.isBlank()) {
            errors.add(ValidationError("branchTemplate", "Branch template is required", "REQUIRED"))
        } else if (input.branchTemplate.length > 200) {
            errors.add(ValidationError("branchTemplate", "Branch template must not exceed 200 characters", "MAX_LENGTH"))
        } else if (!input.branchTemplate.contains("{taskId}")) {
            errors.add(ValidationError("branchTemplate", "Branch template must contain {taskId} placeholder", "MISSING_PLACEHOLDER"))
        }
        
        // Validate description
        input.description?.let { desc ->
            if (desc.length > 10000) {
                errors.add(ValidationError("description", "Description must not exceed 10000 characters", "MAX_LENGTH"))
            }
        }
        
        // Validate team
        input.team?.let { team ->
            if (team.length > 200) {
                errors.add(ValidationError("team", "Team name must not exceed 200 characters", "MAX_LENGTH"))
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
}

