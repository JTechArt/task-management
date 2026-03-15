package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Scope at which a rule can be applied
 */
enum class RuleScope {
    GLOBAL,      // Applied to all projects
    PROJECT,     // Applied to specific projects
    REPOSITORY,  // Applied to specific repositories
    IDE          // Applied to specific IDE types
}

/**
 * Category of rule for organization and filtering
 */
enum class RuleCategory {
    CODING_STANDARDS,
    ARCHITECTURE,
    TESTING,
    DOCUMENTATION,
    AI_ASSISTANT
}

/**
 * Represents a reusable rule set for IDE and AI tools
 */
data class Rule(
    val id: UUID,
    val name: String,
    val content: String,
    val category: RuleCategory?,
    val scope: RuleScope,
    val targetIDE: IDEType?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null
) {
    val isArchived: Boolean
        get() = archivedAt != null
    
    fun update(
        name: String? = null,
        content: String? = null,
        category: RuleCategory? = null,
        scope: RuleScope? = null,
        targetIDE: IDEType? = null
    ): Rule = copy(
        name = name ?: this.name,
        content = content ?: this.content,
        category = category ?: this.category,
        scope = scope ?: this.scope,
        targetIDE = targetIDE ?: this.targetIDE,
        updatedAt = Instant.now()
    )
    
    fun archive(): Rule = copy(
        archivedAt = Instant.now(),
        updatedAt = Instant.now()
    )
}

/**
 * Request to create a new rule
 */
data class CreateRuleRequest(
    val name: String,
    val content: String,
    val category: RuleCategory?,
    val scope: RuleScope,
    val targetIDE: IDEType?
)

/**
 * Request to update an existing rule
 */
data class UpdateRuleRequest(
    val id: UUID,
    val name: String?,
    val content: String?,
    val category: RuleCategory?,
    val scope: RuleScope?,
    val targetIDE: IDEType?
)

/**
 * Request to attach a rule to a project
 */
data class AttachRuleRequest(
    val ruleId: UUID,
    val projectId: UUID
)

/**
 * Request to detach a rule from a project
 */
data class DetachRuleRequest(
    val ruleId: UUID,
    val projectId: UUID
)

/**
 * Represents a rule attached to a project
 */
data class ProjectRule(
    val projectId: UUID,
    val ruleId: UUID,
    val appliedAt: Instant
)

/**
 * Rule export format for import/export functionality
 */
data class RuleExport(
    val name: String,
    val content: String,
    val category: RuleCategory?,
    val scope: RuleScope,
    val targetIDE: IDEType?
)

