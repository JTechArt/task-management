package com.aitask.core.domain.service

import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskEvent
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.SlackChannelRepository
import com.aitask.core.infrastructure.slack.SlackDeliveryException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class SlackNotificationServiceTest {

    @Test
    fun `notifyTaskEvent sends to configured channels and records activity`() = runTest {
        val slackService = mockk<SlackService>()
        val slackChannelRepository = mockk<SlackChannelRepository>()
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/tmp/ws",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val task = Task(
            id = taskId,
            title = "Test Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = null,
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val channel = SlackChannelConfig(
            id = UUID.randomUUID(),
            webhookUrl = "https://hooks.slack.com/services/T/B/X",
            channelDisplayName = "#test",
            projectId = projectId,
            enabledEvents = setOf(TaskEvent.TASK_CREATED),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { slackChannelRepository.findByProjectAndEvent(projectId, TaskEvent.TASK_CREATED) } returns listOf(channel)
        coEvery { slackService.sendToWebhook(any(), any(), any()) } returns Result.success(Unit)
        coEvery { activityRepository.create(any()) } returns mockk()
        val service = SlackNotificationService(
            slackService = slackService,
            slackChannelRepository = slackChannelRepository,
            activityRepository = activityRepository
        )
        service.notifyTaskEvent(TaskEvent.TASK_CREATED, task, project)
        coVerify { slackService.sendToWebhook(channel.webhookUrl, any(), any()) }
        coVerify { activityRepository.create(match { activity ->
            activity.type == ActivityType.NOTIFICATION_SENT &&
                activity.entityId == taskId &&
                activity.status == ActivityStatus.SUCCESS &&
                activity.projectId == projectId
        }) }
    }

    @Test
    fun `notifyTaskEvent records failed activity when send fails`() = runTest {
        val slackService = mockk<SlackService>()
        val slackChannelRepository = mockk<SlackChannelRepository>()
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test",
            description = null,
            workspacePath = "/tmp",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val task = Task(
            id = taskId,
            title = "Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = null,
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val channel = SlackChannelConfig(
            id = UUID.randomUUID(),
            webhookUrl = "https://hooks.slack.com/services/T/B/X",
            channelDisplayName = "#test",
            projectId = projectId,
            enabledEvents = setOf(TaskEvent.TASK_CREATED),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { slackChannelRepository.findByProjectAndEvent(projectId, TaskEvent.TASK_CREATED) } returns listOf(channel)
        coEvery { slackService.sendToWebhook(any(), any(), any()) } returns Result.failure(SlackDeliveryException("Network error"))
        coEvery { activityRepository.create(any()) } returns mockk()
        val service = SlackNotificationService(
            slackService = slackService,
            slackChannelRepository = slackChannelRepository,
            activityRepository = activityRepository
        )
        service.notifyTaskEvent(TaskEvent.TASK_CREATED, task, project)
        coVerify { activityRepository.create(match { activity ->
            activity.type == ActivityType.NOTIFICATION_SENT && activity.status == ActivityStatus.FAILED
        }) }
    }
}
