package com.aitask.core.domain.service

/**
 * Sends Slack notifications via Incoming Webhook.
 * Implementations must not block or throw; failures are reported via Result.
 */
interface SlackService {
    /**
     * Send a message to the given webhook URL.
     * @param webhookUrl Slack Incoming Webhook URL (sensitive)
     * @param text Plain text message body
     * @param blocks Optional Slack Block Kit blocks (JSON)
     * @return Success or failure; implementations should not throw
     */
    suspend fun sendToWebhook(
        webhookUrl: String,
        text: String,
        blocks: List<SlackBlock>? = null
    ): Result<Unit>
}

/**
 * Simplified Slack Block Kit block (for attachments/formatting)
 */
data class SlackBlock(
    val type: String,
    val text: String? = null,
    val elements: List<SlackBlockElement>? = null
)

data class SlackBlockElement(
    val type: String,
    val text: String
)
