package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.CreateRuleRequest
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.validation.RuleValidator
import java.time.Instant
import java.util.UUID

class CreateRuleUseCase(
    private val ruleRepository: RuleRepository,
    private val ruleValidator: RuleValidator
) {
    suspend operator fun invoke(request: CreateRuleRequest): Result<Rule> {
        return try {
            // Validate rule
            val validation = ruleValidator.validate(request)
            if (!validation.isValid) {
                return Result.failure(
                    ValidationException("Rule validation failed: ${validation.errors.joinToString { it.message }}")
                )
            }
            
            // Create rule
            val now = Instant.now()
            val rule = Rule(
                id = UUID.randomUUID(),
                name = request.name,
                content = request.content,
                category = request.category,
                scope = request.scope,
                targetIDE = request.targetIDE,
                createdAt = now,
                updatedAt = now
            )
            
            val createdRule = ruleRepository.create(rule)
            Result.success(createdRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

