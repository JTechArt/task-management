package com.aitask.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SlackConversationSummaryItem(
    val analysisDay: String,
    val channelId: String,
    val topicLabel: String,
    val category: String,
    val summaryText: String,
    val channelUrl: String,
    val messageUrl: String?,
    val threadUrl: String?,
    val threadRootTimestamp: String?,
    val sourceMessageTimestamp: String?,
    val degradedSingleTopicFallback: Boolean
)
