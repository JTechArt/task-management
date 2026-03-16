package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.TaskEvent
import com.aitask.core.domain.repository.SlackChannelRepository
import java.util.UUID

data class UpdateSlackChannelRequest(
    val webhookUrl: String? = null,
    val channelDisplayName: String? = null,
    val enabledEvents: Set<TaskEvent>? = null
)

class UpdateSlackChannelUseCase(
    private val slackChannelRepository: SlackChannelRepository
) {
    suspend operator fun invoke(
        id: UUID,
        request: UpdateSlackChannelRequest
    ): Result<SlackChannelConfig> {
        val existing = slackChannelRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Slack channel config not found: $id"))
        if (request.webhookUrl != null && !request.webhookUrl.matches(Regex("^https://hooks\\.slack\\.com/services/.*"))) {
            return Result.failure(IllegalArgumentException("Invalid Slack webhook URL format"))
        }
        val updated = existing.copy(
            webhookUrl = request.webhookUrl ?: existing.webhookUrl,
            channelDisplayName = request.channelDisplayName ?: existing.channelDisplayName,
            enabledEvents = request.enabledEvents ?: existing.enabledEvents,
            updatedAt = java.time.Instant.now()
        )
        return Result.success(slackChannelRepository.update(updated))
    }
}
