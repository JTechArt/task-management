package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.UpdateRuleRequest
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.validation.RuleValidator

class UpdateRuleUseCase(
    private val ruleRepository: RuleRepository,
    private val ruleValidator: RuleValidator
) {
    suspend operator fun invoke(request: UpdateRuleRequest): Result<Rule> {
        return try {
            // Validate update request
            val validation = ruleValidator.validate(request)
            if (!validation.isValid) {
                return Result.failure(
                    ValidationException("Rule validation failed: ${validation.errors.joinToString { it.message }}")
                )
            }
            
            // Find existing rule
            val existing = ruleRepository.findById(request.id)
                ?: return Result.failure(RuleNotFoundException("Rule with id ${request.id} not found"))
            
            if (existing.isArchived) {
                return Result.failure(ArchivedRuleException("Cannot update archived rule"))
            }
            
            // Update rule
            val updated = existing.update(
                name = request.name,
                content = request.content,
                category = request.category,
                scope = request.scope,
                targetIDE = request.targetIDE
            )
            
            val savedRule = ruleRepository.update(updated)
            Result.success(savedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RuleNotFoundException(message: String) : Exception(message)
class ArchivedRuleException(message: String) : Exception(message)

