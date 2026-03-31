package com.aitask.core.domain.service

import com.aitask.core.domain.model.SlackConversationSummaryItem

enum class SlackAnalysisTrigger {
    MANUAL,
    SCHEDULED
}

sealed class SlackChannelRunOutcome {
    data class SkippedNoNewContent(val channelId: String) : SlackChannelRunOutcome()
    data class Processed(
        val channelId: String,
        val messageCount: Int,
        val summaries: List<SlackConversationSummaryItem>,
        val summaryDiagnostics: List<String> = emptyList()
    ) : SlackChannelRunOutcome()
    data class Failed(val channelId: String, val diagnostic: String) : SlackChannelRunOutcome()
}

data class SlackAnalysisRunResult(
    val outcomes: List<SlackChannelRunOutcome>,
    val summaryMessage: String
)

/**
 * Emitted while a run is in progress so the UI can show which channel is active and which are queued.
 */
data class SlackAnalysisChannelProgress(
    val channelBeingProcessed: String?,
    val pendingChannelIds: List<String>
)

interface SlackChannelAnalysisService {
    suspend fun runIncrementalAnalysis(
        trigger: SlackAnalysisTrigger,
        onProgress: (String) -> Unit,
        onChannelProgress: (SlackAnalysisChannelProgress) -> Unit = {}
    ): Result<SlackAnalysisRunResult>
}
