package com.aitask.core.domain.model

import java.util.UUID

/**
 * Task lifecycle events that can trigger Slack notifications
 */
enum class TaskEvent {
    TASK_CREATED,
    TASK_STARTED,
    TASK_COMPLETED,
    WORKSPACE_CREATED,
    IDE_LAUNCHED
}

/**
 * Slack channel configuration for a project (Incoming Webhook).
 * Webhook URL is sensitive; do not log or expose in UI.
 */
data class SlackChannelConfig(
    val id: UUID,
    val webhookUrl: String,
    val channelDisplayName: String,
    val projectId: UUID,
    val enabledEvents: Set<TaskEvent>,
    val createdAt: java.time.Instant,
    val updatedAt: java.time.Instant
) {
    fun hasEventEnabled(event: TaskEvent): Boolean = enabledEvents.contains(event)

    override fun toString(): String =
        "SlackChannelConfig(id=$id, channelDisplayName=$channelDisplayName, projectId=$projectId, enabledEvents=$enabledEvents)"
}
