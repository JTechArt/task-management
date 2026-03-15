package com.aitask.core.domain.validation

import com.aitask.core.domain.model.CreateRepositoryRequest

class RepositoryValidator : Validator<CreateRepositoryRequest> {
    override fun validate(input: CreateRepositoryRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate name
        if (input.name.isBlank()) {
            errors.add(ValidationError("name", "Repository name is required", "REQUIRED"))
        } else if (input.name.length > 200) {
            errors.add(ValidationError("name", "Repository name must not exceed 200 characters", "MAX_LENGTH"))
        }
        
        // Validate clone URL
        if (input.cloneUrl.isBlank()) {
            errors.add(ValidationError("cloneUrl", "Clone URL is required", "REQUIRED"))
        } else if (input.cloneUrl.length > 500) {
            errors.add(ValidationError("cloneUrl", "Clone URL must not exceed 500 characters", "MAX_LENGTH"))
        } else if (!isValidGitUrl(input.cloneUrl)) {
            errors.add(ValidationError("cloneUrl", "Clone URL must be a valid Git URL (e.g., https://github.com/user/repo.git or git@github.com:user/repo.git)", "INVALID_URL"))
        }
        
        // Validate preferred IDEs
        if (input.preferredIDEs.isEmpty()) {
            errors.add(ValidationError("preferredIDEs", "At least one preferred IDE must be selected", "REQUIRED"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(errors)
        }
    }
    
    private fun isValidGitUrl(url: String): Boolean {
        // Support multiple Git URL formats:
        // - https://github.com/user/repo.git
        // - git://github.com/user/repo.git
        // - ssh://git@github.com/user/repo.git
        // - git@github.com:user/repo.git (SSH shorthand)
        val protocolPattern = Regex("^(https?|git|ssh)://.*", RegexOption.IGNORE_CASE)
        val sshShorthandPattern = Regex("^[a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+:.*", RegexOption.IGNORE_CASE)
        return protocolPattern.matches(url) || sshShorthandPattern.matches(url)
    }
}

