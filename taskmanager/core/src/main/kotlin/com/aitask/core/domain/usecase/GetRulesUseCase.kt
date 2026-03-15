package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.repository.RuleRepository
import java.util.UUID

class GetRulesUseCase(
    private val ruleRepository: RuleRepository
) {
    suspend fun getAll(includeArchived: Boolean = false): Result<List<Rule>> {
        return try {
            val rules = ruleRepository.findAll()
            val filtered = if (includeArchived) rules else rules.filter { !it.isArchived }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getByScope(scope: RuleScope, includeArchived: Boolean = false): Result<List<Rule>> {
        return try {
            val rules = ruleRepository.findByScope(scope)
            val filtered = if (includeArchived) rules else rules.filter { !it.isArchived }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getByProject(projectId: UUID, includeArchived: Boolean = false): Result<List<Rule>> {
        return try {
            val rules = ruleRepository.findByProject(projectId)
            val filtered = if (includeArchived) rules else rules.filter { !it.isArchived }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getById(ruleId: UUID): Result<Rule> {
        return try {
            val rule = ruleRepository.findById(ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with id $ruleId not found"))
            Result.success(rule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

