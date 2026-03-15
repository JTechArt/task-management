package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Result of applying rules to a workspace
 */
data class AppliedRules(
    val workspacePath: String,
    val projectId: UUID,
    val ideType: IDEType?,
    val appliedRuleIds: List<UUID>,
    val skippedRules: List<SkippedRule>,
    val appliedAt: Instant,
    val success: Boolean,
    val errorMessage: String? = null
) {
    val totalRulesApplied: Int
        get() = appliedRuleIds.size
    
    val totalRulesSkipped: Int
        get() = skippedRules.size
    
    val hasErrors: Boolean
        get() = !success || errorMessage != null
}

/**
 * Information about a rule that was skipped during application
 */
data class SkippedRule(
    val ruleId: UUID,
    val ruleName: String,
    val reason: SkipReason,
    val details: String? = null
)

/**
 * Reason why a rule was skipped
 */
enum class SkipReason {
    IDE_MISMATCH,           // Rule targets different IDE
    SCOPE_MISMATCH,         // Rule scope doesn't match context
    ALREADY_APPLIED,        // Rule already applied
    APPLICATION_ERROR,      // Error occurred during application
    ARCHIVED                // Rule is archived
}

/**
 * Request to apply rules to a workspace
 */
data class ApplyRulesRequest(
    val workspacePath: String,
    val projectId: UUID,
    val ideType: IDEType?,
    val selectedRepositories: List<UUID> = emptyList()
)

/**
 * Exception thrown when rule application fails
 */
class RuleApplicationException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)

