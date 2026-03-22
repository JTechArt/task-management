package com.aitask.core.domain.service

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskEvent
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.SlackChannelRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Orchestrates Slack notification delivery for task lifecycle events.
 * Sends notifications asynchronously without blocking the underlying action (AC4).
 */
class SlackNotificationService(
    private val slackService: SlackService,
    private val slackChannelRepository: SlackChannelRepository,
    private val activityRepository: ActivityRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * Fire-and-forget: launch notification delivery in background.
     * Does not block; failures are recorded in activity (AC4, AC5).
     */
    fun notifyInBackground(event: TaskEvent, task: Task, project: Project) {
        scope.launch {
            notifyTaskEvent(event, task, project)
        }
    }

    /**
     * Send notifications to all configured channels for this project and event.
     * Records success/failure in activity log (AC5).
     */
    suspend fun notifyTaskEvent(event: TaskEvent, task: Task, project: Project) {
        val channels = slackChannelRepository.findByProjectAndEvent(project.id, event)
        if (channels.isEmpty()) return
        val message = buildMessage(event, task, project)
        channels.forEach { channel ->
            val result = slackService.sendToWebhook(channel.webhookUrl, message.text, message.blocks)
            recordNotificationActivity(
                task = task,
                projectId = project.id,
                channelName = channel.channelDisplayName,
                success = result.isSuccess,
                errorMessage = result.exceptionOrNull()?.message
            )
            if (result.isFailure) {
                logger.warn { "Slack notification failed for ${channel.channelDisplayName}: ${result.exceptionOrNull()?.message}" }
            }
        }
    }

    private fun buildMessage(
        event: TaskEvent,
        task: Task,
        project: Project
    ): SlackMessagePayload {
        val eventLabel = event.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        val text = "[$eventLabel] *${task.title}* in project *${project.name}* (Task: ${task.id}, Status: ${task.status})"
        return SlackMessagePayload(text = text, blocks = null)
    }

    private suspend fun recordNotificationActivity(
        task: Task,
        projectId: UUID,
        channelName: String,
        success: Boolean,
        errorMessage: String?
    ) {
        val description = if (success) {
            "Slack notification sent to $channelName for task: ${task.title}"
        } else {
            "Slack notification failed for $channelName: ${errorMessage ?: "Unknown error"}"
        }
        val status = if (success) ActivityStatus.SUCCESS else ActivityStatus.FAILED
        val metadata = mutableMapOf(
            "channel" to channelName,
            "taskId" to task.id.toString(),
            "taskTitle" to task.title
        )
        errorMessage?.let { metadata["error"] = it }
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.NOTIFICATION_SENT,
                entityType = "task",
                entityId = task.id,
                description = description,
                metadata = metadata,
                createdAt = Instant.now(),
                status = status,
                projectId = projectId
            )
        )
    }
}

private data class SlackMessagePayload(
    val text: String,
    val blocks: List<SlackBlock>?
)
