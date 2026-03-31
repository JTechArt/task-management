package com.aitask.core.domain.service

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.SlackChannelMessage
import com.aitask.core.domain.model.SlackConversationSummaryItem

interface SlackSummaryGenerationService {
    suspend fun summarizeChannelDays(
        channelId: String,
        messagesByDay: Map<String, List<SlackChannelMessage>>,
        configuration: LlmConfiguration?,
        apiKey: String?
    ): SlackChannelSummaryBatch
}

data class SlackChannelSummaryBatch(
    val items: List<SlackConversationSummaryItem>,
    val diagnostics: List<String>
)
