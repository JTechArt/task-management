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
    IDE_LAUNCHED,
    PROJECT_CREATED,
    PROJECT_UPDATED,
    REPOSITORY_ADDED
}

/**
 * Represents an activity log entry
 */
data class Activity(
    val id: UUID,
    val type: ActivityType,
    val entityType: String, // "task", "project", "workspace", etc.
    val entityId: UUID,
    val description: String,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant
)

/**
 * Request to create an activity log entry
 */
data class CreateActivityRequest(
    val type: ActivityType,
    val entityType: String,
    val entityId: UUID,
    val description: String,
    val metadata: Map<String, String> = emptyMap()
)

