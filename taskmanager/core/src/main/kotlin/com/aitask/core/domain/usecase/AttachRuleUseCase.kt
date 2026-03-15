package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AttachRuleRequest
import com.aitask.core.domain.model.ProjectRule
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RuleRepository

class AttachRuleUseCase(
    private val ruleRepository: RuleRepository,
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(request: AttachRuleRequest): Result<ProjectRule> {
        return try {
            // Verify rule exists and is not archived
            val rule = ruleRepository.findById(request.ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with id ${request.ruleId} not found"))
            
            if (rule.isArchived) {
                return Result.failure(ArchivedRuleException("Cannot attach archived rule"))
            }
            
            // Verify project exists and is not archived
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project with id ${request.projectId} not found"))
            
            if (project.isArchived) {
                return Result.failure(ArchivedProjectException("Cannot attach rule to archived project"))
            }
            
            // Check if already attached
            val existingRules = ruleRepository.findByProject(request.projectId)
            if (existingRules.any { it.id == request.ruleId }) {
                return Result.failure(RuleAlreadyAttachedException("Rule is already attached to this project"))
            }
            
            // Attach rule to project
            val projectRule = ruleRepository.attachToProject(request.projectId, request.ruleId)
            Result.success(projectRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RuleAlreadyAttachedException(message: String) : Exception(message)

