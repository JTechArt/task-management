package com.aitask.core.domain.model

import kotlinx.serialization.Serializable

private const val SCHEMA_VERSION = 1

/**
 * Root export format for projects, tasks, repositories, and rules.
 * Credentials excluded per Export Security (architecture).
 */
@Serializable
data class DataExportBundle(
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val projects: List<ProjectExportItem> = emptyList(),
    val tasks: List<TaskExportItem> = emptyList(),
    val repositories: List<RepositoryExportItem> = emptyList(),
    val rules: List<RuleExportItem> = emptyList(),
    val projectRules: List<ProjectRuleExportItem> = emptyList()
)

@Serializable
data class ProjectExportItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val workspacePath: String,
    val branchTemplate: String = "task-{taskId}",
    val retentionPolicy: String = "KEEP_ALL",
    val tags: List<String> = emptyList(),
    val team: String? = null,
    val archivedAt: String? = null
)

@Serializable
data class TaskExportItem(
    val id: String,
    val projectId: String,
    val title: String,
    val description: String? = null,
    val taskType: String,
    val status: String,
    val workspacePath: String? = null,
    val branchName: String? = null,
    val workspaceCleanedAt: String? = null,
    val completedAt: String? = null
)

@Serializable
data class RepositoryExportItem(
    val id: String,
    val projectId: String,
    val name: String,
    val cloneUrl: String,
    val provider: String,
    val authType: String,
    val preferredIDEs: List<String> = emptyList(),
    val isPrimary: Boolean = false
)

@Serializable
data class RuleExportItem(
    val id: String,
    val name: String,
    val content: String,
    val category: String? = null,
    val scope: String,
    val targetIDE: String? = null,
    val archivedAt: String? = null
)

@Serializable
data class ProjectRuleExportItem(
    val projectId: String,
    val ruleId: String
)

/**
 * Result of import validation. AC3: report conflicts before applying.
 */
data class ImportValidationResult(
    val isValid: Boolean,
    val conflicts: List<ImportConflict>
)

/**
 * A single conflict detected during import validation.
 */
data class ImportConflict(
    val entityType: String,
    val identifier: String,
    val message: String
)

/**
 * Result of import apply. AC5: clear summary of processed records.
 */
data class ImportResult(
    val projectsCreated: Int,
    val tasksCreated: Int,
    val reposCreated: Int,
    val rulesCreated: Int,
    val projectRulesLinked: Int,
    val conflicts: List<ImportConflict>,
    val errors: List<String>
)
