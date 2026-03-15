package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.RuleRepository
import java.util.UUID

class DeleteRuleUseCase(
    private val ruleRepository: RuleRepository
) {
    suspend operator fun invoke(ruleId: UUID): Result<Unit> {
        return try {
            // Verify rule exists
            val rule = ruleRepository.findById(ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with id $ruleId not found"))
            
            // Archive instead of hard delete to preserve history
            val archived = rule.archive()
            ruleRepository.update(archived)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

