package com.aitask.core.infrastructure.slack

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.SlackChannelMessage
import com.aitask.core.domain.model.SlackConversationSummaryItem
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.SlackChannelAnalyzerPluginIds
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.SlackAnalysisRunResult
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.core.domain.service.SlackChannelAnalysisService
import com.aitask.core.domain.service.SlackChannelRunOutcome
import com.aitask.core.domain.service.SlackSummaryGenerationService
import com.aitask.core.infrastructure.plugin.SlackConversationHistoryResult
import com.aitask.core.infrastructure.plugin.SlackWebApiClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@Serializable
private data class ChannelCheckpointsFile(val channels: Map<String, String> = emptyMap())

class DefaultSlackChannelAnalysisService(
    private val pluginManagementService: PluginManagementService,
    private val activityRepository: ActivityRepository,
    private val slackWebApiClient: SlackWebApiClient,
    private val llmConfigurationRepository: LlmConfigurationRepository,
    private val slackSummaryGenerationService: SlackSummaryGenerationService
) : SlackChannelAnalysisService {
    private val mutex: Mutex = Mutex()
    private val checkpointJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val summaryJson: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun runIncrementalAnalysis(
        trigger: SlackAnalysisTrigger,
        onProgress: (String) -> Unit
    ): Result<SlackAnalysisRunResult> = mutex.withLock {
        try {
            onProgress("Loading configuration…")
            val initialSnapshot: PluginConfigurationSnapshot = pluginManagementService.getConfiguration(
                pluginId = SlackChannelAnalyzerPluginIds.PLUGIN_ID,
                scope = PluginConfigurationScope.APP,
                scopeKey = null
            ) ?: return@withLock Result.failure(IllegalStateException("Slack analyzer is not configured"))
            val token: String = initialSnapshot.secrets[SlackChannelAnalyzerPluginIds.SECRET_SLACK_BOT_TOKEN]?.trim().orEmpty()
            if (token.isBlank()) {
                return@withLock Result.failure(IllegalStateException("Slack bot token is missing"))
            }
            val rawChannels: String = initialSnapshot.fields[SlackChannelAnalyzerPluginIds.FIELD_ANALYSIS_CHANNELS]?.trim().orEmpty()
            if (rawChannels.isBlank()) {
                return@withLock Result.failure(IllegalStateException("No analysis channels configured"))
            }
            val channelIds: List<String> = rawChannels.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val checkpoints: MutableMap<String, String> = parseCheckpoints(json = initialSnapshot.fields[SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON])
            val outcomes: MutableList<SlackChannelRunOutcome> = mutableListOf()
            var workingSnapshot: PluginConfigurationSnapshot = initialSnapshot
            for (channelId: String in channelIds) {
                onProgress("Analyzing channel $channelId…")
                val lastTs: String? = checkpoints[channelId]
                val collectResult: Pair<List<SlackChannelMessage>, String?> = collectNewMessages(
                    token = token,
                    channelId = channelId,
                    lastCheckpoint = lastTs
                )
                val diagnostic: String? = collectResult.second
                if (diagnostic != null) {
                    outcomes.add(SlackChannelRunOutcome.Failed(channelId = channelId, diagnostic = diagnostic))
                    continue
                }
                val newMessages: List<SlackChannelMessage> = collectResult.first
                if (newMessages.isEmpty()) {
                    outcomes.add(SlackChannelRunOutcome.SkippedNoNewContent(channelId = channelId))
                    continue
                }
                val maxTs: String = newMessages.maxByOrNull { message: SlackChannelMessage -> message.ts.toBigDecimal() }?.ts
                    ?: newMessages.first().ts
                val checkpointBeforeChannel: String? = checkpoints[channelId]
                checkpoints[channelId] = maxTs
                val saved: Result<PluginConfigurationSnapshot> = saveCheckpoints(snapshot = workingSnapshot, checkpoints = checkpoints)
                saved.fold(
                    onSuccess = { updated: PluginConfigurationSnapshot ->
                        workingSnapshot = updated
                        val llmConfig = llmConfigurationRepository.findDefault()
                        val apiKey: String? = if (llmConfig != null && llmConfig.hasApiKey) {
                            llmConfigurationRepository.findApiKey(llmConfig.id)
                        } else {
                            null
                        }
                        val byDay: Map<String, List<SlackChannelMessage>> = groupMessagesByUtcDay(messages = newMessages)
                        val batch = slackSummaryGenerationService.summarizeChannelDays(
                            channelId = channelId,
                            messagesByDay = byDay,
                            configuration = llmConfig,
                            apiKey = apiKey
                        )
                        outcomes.add(
                            SlackChannelRunOutcome.Processed(
                                channelId = channelId,
                                messageCount = newMessages.size,
                                summaries = batch.items,
                                summaryDiagnostics = batch.diagnostics
                            )
                        )
                    },
                    onFailure = { error: Throwable ->
                        if (checkpointBeforeChannel == null) {
                            checkpoints.remove(channelId)
                        } else {
                            checkpoints[channelId] = checkpointBeforeChannel
                        }
                        outcomes.add(
                            SlackChannelRunOutcome.Failed(
                                channelId = channelId,
                                diagnostic = "Failed to save checkpoint: ${error.message ?: "unknown"}"
                            )
                        )
                    }
                )
            }
            val summary: String = buildSummary(outcomes = outcomes)
            recordRunActivity(trigger = trigger, outcomes = outcomes, summary = summary)
            Result.success(SlackAnalysisRunResult(outcomes = outcomes, summaryMessage = summary))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectNewMessages(
        token: String,
        channelId: String,
        lastCheckpoint: String?
    ): Pair<List<SlackChannelMessage>, String?> {
        val collected: MutableList<SlackChannelMessage> = mutableListOf()
        var pageCursor: String? = null
        while (true) {
            val page: SlackConversationHistoryResult = slackWebApiClient.conversationHistory(
                token = token,
                channelId = channelId,
                oldestTs = if (pageCursor == null) lastCheckpoint else null,
                limit = 200,
                cursor = pageCursor
            )
            when (page) {
                is SlackConversationHistoryResult.Error -> return Pair(emptyList(), page.diagnostic)
                is SlackConversationHistoryResult.Ok -> {
                    val filtered: List<SlackChannelMessage> = page.page.messages.filter { message: SlackChannelMessage ->
                        isStrictlyAfter(ts = message.ts, lastCheckpoint = lastCheckpoint)
                    }
                    collected.addAll(filtered)
                    if (!page.page.hasMore || page.page.nextCursor.isNullOrBlank()) {
                        break
                    }
                    pageCursor = page.page.nextCursor
                }
            }
        }
        return Pair(collected, null)
    }

    private fun groupMessagesByUtcDay(messages: List<SlackChannelMessage>): Map<String, List<SlackChannelMessage>> {
        val zone: ZoneId = ZoneId.of("UTC")
        return messages.groupBy { message: SlackChannelMessage ->
            val seconds: Long = message.ts.substringBefore('.').toLong()
            Instant.ofEpochSecond(seconds).atZone(zone).toLocalDate().toString()
        }
    }

    private fun isStrictlyAfter(ts: String, lastCheckpoint: String?): Boolean {
        if (lastCheckpoint == null) {
            return true
        }
        return ts.toBigDecimal() > lastCheckpoint.toBigDecimal()
    }

    private suspend fun saveCheckpoints(
        snapshot: PluginConfigurationSnapshot,
        checkpoints: Map<String, String>
    ): Result<PluginConfigurationSnapshot> {
        val encoded: String = encodeCheckpoints(checkpoints = checkpoints)
        val mergedFields: Map<String, String> = snapshot.fields + (SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON to encoded)
        val updated: PluginConfigurationSnapshot = snapshot.copy(
            fields = mergedFields,
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        return pluginManagementService.saveConfiguration(updated)
    }

    private fun parseCheckpoints(json: String?): MutableMap<String, String> {
        if (json.isNullOrBlank()) {
            return mutableMapOf()
        }
        return try {
            checkpointJson.decodeFromString<ChannelCheckpointsFile>(json).channels.toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun encodeCheckpoints(checkpoints: Map<String, String>): String =
        checkpointJson.encodeToString(ChannelCheckpointsFile(channels = checkpoints))

    private fun buildSummary(outcomes: List<SlackChannelRunOutcome>): String {
        val processed: Int = outcomes.count { outcome: SlackChannelRunOutcome -> outcome is SlackChannelRunOutcome.Processed }
        val skipped: Int = outcomes.count { outcome: SlackChannelRunOutcome -> outcome is SlackChannelRunOutcome.SkippedNoNewContent }
        val failed: Int = outcomes.count { outcome: SlackChannelRunOutcome -> outcome is SlackChannelRunOutcome.Failed }
        val topicSummaries: Int = outcomes.filterIsInstance<SlackChannelRunOutcome.Processed>()
            .sumOf { outcome: SlackChannelRunOutcome.Processed -> outcome.summaries.size }
        return "Slack analysis finished: processed=$processed, skipped (no new messages)=$skipped, channel failures=$failed, topic summaries=$topicSummaries."
    }

    private suspend fun recordRunActivity(
        trigger: SlackAnalysisTrigger,
        outcomes: List<SlackChannelRunOutcome>,
        summary: String
    ) {
        val failures: String = outcomes.filterIsInstance<SlackChannelRunOutcome.Failed>()
            .joinToString(separator = "; ") { failed: SlackChannelRunOutcome.Failed -> "${failed.channelId}: ${failed.diagnostic}" }
        val hasChannelFailure: Boolean = outcomes.any { outcome: SlackChannelRunOutcome -> outcome is SlackChannelRunOutcome.Failed }
        val activityStatus: ActivityStatus = if (hasChannelFailure) ActivityStatus.FAILED else ActivityStatus.SUCCESS
        val allSummaries: List<SlackConversationSummaryItem> = outcomes.filterIsInstance<SlackChannelRunOutcome.Processed>()
            .flatMap { outcome: SlackChannelRunOutcome.Processed -> outcome.summaries }
        val summaryDiagnostics: String = outcomes.filterIsInstance<SlackChannelRunOutcome.Processed>()
            .flatMap { outcome: SlackChannelRunOutcome.Processed -> outcome.summaryDiagnostics }
            .joinToString(separator = " | ")
        val summariesPayload: String = summaryJson.encodeToString(
            ListSerializer(SlackConversationSummaryItem.serializer()),
            allSummaries
        )
        val summariesStored: String = if (summariesPayload.length > 12_000) {
            summariesPayload.take(12_000) + "…"
        } else {
            summariesPayload
        }
        activityRepository.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.SLACK_ANALYSIS_RUN,
                entityType = "plugin",
                entityId = UUID.nameUUIDFromBytes(SlackChannelAnalyzerPluginIds.PLUGIN_ID.toByteArray()),
                description = summary,
                metadata = mapOf(
                    "trigger" to trigger.name,
                    "pluginId" to SlackChannelAnalyzerPluginIds.PLUGIN_ID,
                    "failures" to failures,
                    "summariesJson" to summariesStored,
                    "summaryDiagnostics" to summaryDiagnostics
                ),
                createdAt = Instant.now(),
                status = activityStatus
            )
        )
    }
}
