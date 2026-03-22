package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.TaskEvent
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.SlackChannelRepository
import java.time.Instant
import java.util.UUID

data class CreateSlackChannelRequest(
    val webhookUrl: String,
    val channelDisplayName: String,
    val projectId: UUID,
    val enabledEvents: Set<TaskEvent>
)

class CreateSlackChannelUseCase(
    private val slackChannelRepository: SlackChannelRepository,
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(request: CreateSlackChannelRequest): Result<SlackChannelConfig> {
        val project = projectRepository.findById(request.projectId)
            ?: return Result.failure(ProjectNotFoundException("Project not found: ${request.projectId}"))
        if (project.isArchived) {
            return Result.failure(ArchivedProjectException("Cannot add Slack config to archived project"))
        }
        if (!request.webhookUrl.matches(Regex("^https://hooks\\.slack\\.com/services/.*"))) {
            return Result.failure(IllegalArgumentException("Invalid Slack webhook URL format"))
        }
        val now = Instant.now()
        val config = SlackChannelConfig(
            id = UUID.randomUUID(),
            webhookUrl = request.webhookUrl,
            channelDisplayName = request.channelDisplayName,
            projectId = request.projectId,
            enabledEvents = request.enabledEvents,
            createdAt = now,
            updatedAt = now
        )
        return Result.success(slackChannelRepository.create(config))
    }
}
