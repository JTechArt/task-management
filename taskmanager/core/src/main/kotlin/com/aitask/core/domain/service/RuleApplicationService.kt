package com.aitask.core.domain.service

import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Rule

/**
 * Service for applying rules to workspaces in IDE-compatible formats
 */
interface RuleApplicationService {
    /**
     * Apply rules to a workspace directory
     * 
     * @param workspacePath The path to the workspace directory
     * @param rules The rules to apply
     * @param ideType The IDE type for format compatibility
     * @return Result indicating success or failure with details
     */
    suspend fun applyRulesToWorkspace(
        workspacePath: String,
        rules: List<Rule>,
        ideType: IDEType?
    ): Result<RuleApplicationResult>
    
    /**
     * Get the rule file path for a specific IDE type
     * 
     * @param workspacePath The workspace path
     * @param ideType The IDE type
     * @return The path where rules should be written
     */
    fun getRuleFilePath(workspacePath: String, ideType: IDEType?): String
    
    /**
     * Validate that a workspace can have rules applied
     * 
     * @param workspacePath The workspace path
     * @return true if workspace is valid for rule application
     */
    suspend fun validateWorkspace(workspacePath: String): Boolean
}

/**
 * Result of applying rules to a workspace
 */
data class RuleApplicationResult(
    val filesCreated: List<String>,
    val rulesApplied: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

