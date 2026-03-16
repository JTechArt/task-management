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
    GIT_PREPARED,
    RULES_APPLIED,
    PROJECT_CREATED,
    PROJECT_UPDATED,
    REPOSITORY_ADDED
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

