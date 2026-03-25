package com.aitask.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Type of activity event
 */
enum class ActivityType {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED,
    TASK_STATUS_CHANGED,
    WORKSPACE_CREATED,
    WORKSPACE_PREPARED,
    BMAD_INJECTION_APPLIED,
    BMAD_INJECTION_FAILED,
    WORKSPACE_CLEANUP,
    IDE_LAUNCHED,
    GIT_PREPARED,
    RULES_APPLIED,
    PROJECT_CREATED,
    PROJECT_UPDATED,
    REPOSITORY_ADDED,
    PRE_RUN_SUCCESS,
    PRE_RUN_FAILED,
    NOTIFICATION_SENT,
    BACKUP_CREATED,
    RESTORE_COMPLETED,
    PLUGIN_INSTALLED,
    PLUGIN_ATTACHED,
    PLUGIN_DETACHED,
    PLUGIN_ENABLED,
    PLUGIN_DISABLED,
    PLUGIN_REMOVED,
    PLUGIN_VALIDATED,
    PLUGIN_HEALTH_CHECKED,
    AGENT_EXECUTED,
    LLM_SUGGESTION_USED
}

/**
 * Status of the activity (success, failure, or in progress)
 */
enum class ActivityStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS
}

/**
 * Represents an activity log entry
 */
data class Activity(
    val id: UUID,
    val type: ActivityType,
    val entityType: String,
    val entityId: UUID,
    val description: String,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant,
    val status: ActivityStatus = ActivityStatus.SUCCESS,
    val projectId: UUID? = null
)

/**
 * Request to create an activity log entry
 */
data class CreateActivityRequest(
    val type: ActivityType,
    val entityType: String,
    val entityId: UUID,
    val description: String,
    val metadata: Map<String, String> = emptyMap(),
    val status: ActivityStatus = ActivityStatus.SUCCESS,
    val projectId: UUID? = null
)
