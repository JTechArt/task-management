package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.DetachRuleRequest
import com.aitask.core.domain.repository.RuleRepository

class DetachRuleUseCase(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(request: DetachRuleRequest): Result<Unit> {
        return try {
            // Verify rule exists
            val rule = ruleRepository.findById(request.ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with id ${request.ruleId} not found"))
            
            // Check if rule is attached to project
            val attachedRules = ruleRepository.findByProject(request.projectId)
            if (attachedRules.none { it.id == request.ruleId }) {
                return Result.failure(RuleNotAttachedException("Rule is not attached to this project"))
            }
            
            // Detach rule from project
            ruleRepository.detachFromProject(request.projectId, request.ruleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RuleNotAttachedException(message: String) : Exception(message)

