package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.service.RuleApplicationService
import java.time.Instant
import java.util.UUID

class ApplyRulesToWorkspaceUseCase(
    private val ruleRepository: RuleRepository,
    private val projectRepository: ProjectRepository,
    private val ruleApplicationService: RuleApplicationService,
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(request: ApplyRulesRequest): Result<AppliedRules> {
        return try {
            // Validate project exists
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException("Project with id ${request.projectId} not found"))
            
            // Validate workspace exists
            if (!ruleApplicationService.validateWorkspace(request.workspacePath)) {
                return Result.failure(RuleApplicationException("Invalid workspace path: ${request.workspacePath}"))
            }
            
            // Determine effective rules
            val effectiveRules = determineEffectiveRules(
                projectId = request.projectId,
                ideType = request.ideType
            )
            
            // Filter and categorize rules
            val (applicableRules, skippedRules) = categorizeRules(
                rules = effectiveRules,
                ideType = request.ideType
            )
            
            // Apply rules to workspace
            val applicationResult = if (applicableRules.isNotEmpty()) {
                ruleApplicationService.applyRulesToWorkspace(
                    workspacePath = request.workspacePath,
                    rules = applicableRules,
                    ideType = request.ideType
                )
            } else {
                Result.success(
                    com.aitask.core.domain.service.RuleApplicationResult(
                        filesCreated = emptyList(),
                        rulesApplied = 0,
                        success = true
                    )
                )
            }
            
            // Build result
            val appliedRules = AppliedRules(
                workspacePath = request.workspacePath,
                projectId = request.projectId,
                ideType = request.ideType,
                appliedRuleIds = if (applicationResult.isSuccess) {
                    applicableRules.map { it.id }
                } else {
                    emptyList()
                },
                skippedRules = skippedRules,
                appliedAt = Instant.now(),
                success = applicationResult.isSuccess,
                errorMessage = applicationResult.exceptionOrNull()?.message
            )
            val status = if (applicationResult.isSuccess) ActivityStatus.SUCCESS else ActivityStatus.FAILED
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.RULES_APPLIED,
                    entityType = "project",
                    entityId = request.projectId,
                    description = if (applicationResult.isSuccess) {
                        "Applied ${appliedRules.appliedRuleIds.size} rules to workspace"
                    } else {
                        "Failed to apply rules: ${applicationResult.exceptionOrNull()?.message}"
                    },
                    metadata = mapOf(
                        "workspacePath" to request.workspacePath,
                        "appliedCount" to appliedRules.appliedRuleIds.size.toString()
                    ),
                    status = status,
                    createdAt = Instant.now(),
                    projectId = request.projectId
                )
            )
            Result.success(appliedRules)
        } catch (e: Exception) {
            activityRepository.create(
                Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.RULES_APPLIED,
                    entityType = "project",
                    entityId = request.projectId,
                    description = "Failed to apply rules: ${e.message}",
                    metadata = mapOf("error" to (e.message ?: "Unknown")),
                    status = ActivityStatus.FAILED,
                    createdAt = Instant.now(),
                    projectId = request.projectId
                )
            )
            Result.failure(RuleApplicationException("Failed to apply rules: ${e.message}", e))
        }
    }
    
    /**
     * Determine effective rules for the given context
     */
    private suspend fun determineEffectiveRules(
        projectId: UUID,
        ideType: IDEType?
    ): List<Rule> {
        val rules = mutableListOf<Rule>()
        
        // 1. Get global rules
        val globalRules = ruleRepository.findByScope(RuleScope.GLOBAL)
        rules.addAll(globalRules)
        
        // 2. Get project-specific rules
        val projectRules = ruleRepository.findByProject(projectId)
        rules.addAll(projectRules)
        
        // 3. Get IDE-specific rules if IDE type is specified
        if (ideType != null) {
            val ideRules = ruleRepository.findByScope(RuleScope.IDE)
            rules.addAll(ideRules)
        }
        
        // Remove duplicates (a rule might be both global and attached to project)
        return rules.distinctBy { it.id }
    }
    
    /**
     * Categorize rules into applicable and skipped
     */
    private fun categorizeRules(
        rules: List<Rule>,
        ideType: IDEType?
    ): Pair<List<Rule>, List<SkippedRule>> {
        val applicable = mutableListOf<Rule>()
        val skipped = mutableListOf<SkippedRule>()
        
        for (rule in rules) {
            when {
                rule.isArchived -> {
                    skipped.add(SkippedRule(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        reason = SkipReason.ARCHIVED,
                        details = "Rule is archived"
                    ))
                }
                rule.targetIDE != null && ideType != null && rule.targetIDE != ideType -> {
                    skipped.add(SkippedRule(
                        ruleId = rule.id,
                        ruleName = rule.name,
                        reason = SkipReason.IDE_MISMATCH,
                        details = "Rule targets ${rule.targetIDE}, but workspace uses $ideType"
                    ))
                }
                else -> {
                    applicable.add(rule)
                }
            }
        }
        
        return Pair(applicable, skipped)
    }
}
