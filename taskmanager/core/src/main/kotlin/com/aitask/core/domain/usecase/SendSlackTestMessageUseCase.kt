package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.SlackChannelRepository
import com.aitask.core.domain.service.SlackService
import java.util.UUID

/**
 * Send a test message to a configured Slack channel.
 * Used by the UI to verify webhook configuration.
 */
class SendSlackTestMessageUseCase(
    private val slackChannelRepository: SlackChannelRepository,
    private val slackService: SlackService
) {
    suspend operator fun invoke(channelConfigId: UUID): Result<Unit> {
        val config = slackChannelRepository.findById(channelConfigId)
            ?: return Result.failure(IllegalArgumentException("Slack channel config not found: $channelConfigId"))
        val result = slackService.sendToWebhook(
            webhookUrl = config.webhookUrl,
            text = "🧪 AiTask test notification – your Slack integration is working!"
        )
        return result
    }
}
