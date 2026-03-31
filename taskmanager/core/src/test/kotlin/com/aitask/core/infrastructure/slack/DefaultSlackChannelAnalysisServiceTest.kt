package com.aitask.core.infrastructure.slack

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.PluginConfigurationValidationStatus
import com.aitask.core.domain.plugin.SlackChannelAnalyzerPluginIds
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.core.domain.service.SlackChannelRunOutcome
import com.aitask.core.infrastructure.plugin.InMemoryPluginManagementService
import com.aitask.core.infrastructure.plugin.SlackConversationHistoryPage
import com.aitask.core.infrastructure.plugin.SlackConversationHistoryResult
import com.aitask.core.infrastructure.plugin.SlackWebApiClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class SaveFirstFailPluginManagement(
    private val inner: PluginManagementService
) : PluginManagementService by inner {
    private var failedOnce: Boolean = false
    override suspend fun saveConfiguration(snapshot: PluginConfigurationSnapshot): Result<PluginConfigurationSnapshot> {
        if (!failedOnce) {
            failedOnce = true
            return Result.failure(RuntimeException("simulated save failure"))
        }
        return inner.saveConfiguration(snapshot)
    }
}

class DefaultSlackChannelAnalysisServiceTest {

    private class FakeSlackHistoryClient(
        private val responsesByChannel: Map<String, List<SlackConversationHistoryResult>>
    ) : SlackWebApiClient {
        private val callIndex: MutableMap<String, Int> = mutableMapOf()
        override fun authTest(token: String): Boolean = true
        override fun conversationInfoOk(token: String, channelId: String): Boolean = true
        override fun conversationHistory(
            token: String,
            channelId: String,
            oldestTs: String?,
            limit: Int,
            cursor: String?
        ): SlackConversationHistoryResult {
            val list: List<SlackConversationHistoryResult> = responsesByChannel[channelId]
                ?: listOf(
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = emptyList(),
                            hasMore = false,
                            nextCursor = null
                        )
                    )
                )
            val index: Int = callIndex.getOrDefault(channelId, 0)
            callIndex[channelId] = index + 1
            return list.getOrElse(index) { list.last() }
        }
    }

    private suspend fun seedValidSlackConfig(service: InMemoryPluginManagementService) {
        service.saveConfiguration(
            PluginConfigurationSnapshot(
                pluginId = SlackChannelAnalyzerPluginIds.PLUGIN_ID,
                scope = PluginConfigurationScope.APP,
                fields = mapOf(
                    "analysis_channels" to "C1",
                    "schedule_mode" to "manual_only"
                ),
                secrets = mapOf(SlackChannelAnalyzerPluginIds.SECRET_SLACK_BOT_TOKEN to "xoxb-test"),
                validationStatus = PluginConfigurationValidationStatus.VALID
            )
        )
    }

    @Test
    fun `second run skips when no messages are newer than checkpoint`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val pluginManagementService = InMemoryPluginManagementService(activityRepository = activityRepository)
        seedValidSlackConfig(pluginManagementService)
        val client = FakeSlackHistoryClient(
            responsesByChannel = mapOf(
                "C1" to listOf(
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = listOf("100.0", "101.0"),
                            hasMore = false,
                            nextCursor = null
                        )
                    ),
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = listOf("100.0", "101.0"),
                            hasMore = false,
                            nextCursor = null
                        )
                    )
                )
            )
        )
        val service = DefaultSlackChannelAnalysisService(
            pluginManagementService = pluginManagementService,
            activityRepository = activityRepository,
            slackWebApiClient = client
        )
        val first = service.runIncrementalAnalysis(SlackAnalysisTrigger.MANUAL) { }
        assertTrue(first.isSuccess)
        val firstOutcome = first.getOrNull()!!.outcomes.single() as SlackChannelRunOutcome.Processed
        assertEquals(2, firstOutcome.messageCount)
        val second = service.runIncrementalAnalysis(SlackAnalysisTrigger.MANUAL) { }
        assertTrue(second.isSuccess)
        assertTrue(second.getOrNull()!!.outcomes.single() is SlackChannelRunOutcome.SkippedNoNewContent)
    }

    @Test
    fun `continues when a channel fails`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val pluginManagementService = InMemoryPluginManagementService(activityRepository = activityRepository)
        pluginManagementService.saveConfiguration(
            PluginConfigurationSnapshot(
                pluginId = SlackChannelAnalyzerPluginIds.PLUGIN_ID,
                scope = PluginConfigurationScope.APP,
                fields = mapOf(
                    "analysis_channels" to "C1, C2",
                    "schedule_mode" to "manual_only"
                ),
                secrets = mapOf(SlackChannelAnalyzerPluginIds.SECRET_SLACK_BOT_TOKEN to "xoxb-test"),
                validationStatus = PluginConfigurationValidationStatus.VALID
            )
        )
        val client = FakeSlackHistoryClient(
            responsesByChannel = mapOf(
                "C1" to listOf(
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = listOf("10.0"),
                            hasMore = false,
                            nextCursor = null
                        )
                    )
                ),
                "C2" to listOf(
                    SlackConversationHistoryResult.Error(diagnostic = "channel_not_found")
                )
            )
        )
        val service = DefaultSlackChannelAnalysisService(
            pluginManagementService = pluginManagementService,
            activityRepository = activityRepository,
            slackWebApiClient = client
        )
        val result = service.runIncrementalAnalysis(SlackAnalysisTrigger.MANUAL) { }
        assertTrue(result.isSuccess)
        val outcomes = result.getOrNull()!!.outcomes
        assertEquals(2, outcomes.size)
        assertTrue(outcomes.any { it is SlackChannelRunOutcome.Processed })
        assertTrue(outcomes.any { it is SlackChannelRunOutcome.Failed })
        val activity = activityRepository.findByType(ActivityType.SLACK_ANALYSIS_RUN).single()
        assertEquals(ActivityStatus.FAILED, activity.status)
    }

    @Test
    fun `save checkpoint failure rolls back in memory and does not persist new ts`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val inner = InMemoryPluginManagementService(activityRepository = activityRepository)
        inner.saveConfiguration(
            PluginConfigurationSnapshot(
                pluginId = SlackChannelAnalyzerPluginIds.PLUGIN_ID,
                scope = PluginConfigurationScope.APP,
                fields = mapOf(
                    "analysis_channels" to "C1",
                    "schedule_mode" to "manual_only",
                    SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON to """{"channels":{"C1":"50.0"}}"""
                ),
                secrets = mapOf(SlackChannelAnalyzerPluginIds.SECRET_SLACK_BOT_TOKEN to "xoxb-test"),
                validationStatus = PluginConfigurationValidationStatus.VALID
            )
        )
        val wrapped = SaveFirstFailPluginManagement(inner)
        val client = FakeSlackHistoryClient(
            responsesByChannel = mapOf(
                "C1" to listOf(
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = listOf("100.0"),
                            hasMore = false,
                            nextCursor = null
                        )
                    )
                )
            )
        )
        val service = DefaultSlackChannelAnalysisService(
            pluginManagementService = wrapped,
            activityRepository = activityRepository,
            slackWebApiClient = client
        )
        val result = service.runIncrementalAnalysis(SlackAnalysisTrigger.MANUAL) { }
        assertTrue(result.isSuccess)
        val failedOutcome = result.getOrNull()!!.outcomes.single() as SlackChannelRunOutcome.Failed
        assertTrue(failedOutcome.diagnostic.contains("Failed to save checkpoint"))
        val persisted: String? = inner.getConfiguration(
            pluginId = SlackChannelAnalyzerPluginIds.PLUGIN_ID,
            scope = PluginConfigurationScope.APP,
            scopeKey = null
        )?.fields?.get(SlackChannelAnalyzerPluginIds.FIELD_CHANNEL_CHECKPOINTS_JSON)
        assertTrue(persisted != null && persisted.contains("50.0"))
        assertFalse(persisted!!.contains("100.0"))
    }

    @Test
    fun `records slack analysis activity`() = runTest {
        val activityRepository = InMemoryActivityRepository()
        val pluginManagementService = InMemoryPluginManagementService(activityRepository = activityRepository)
        seedValidSlackConfig(pluginManagementService)
        val client = FakeSlackHistoryClient(
            responsesByChannel = mapOf(
                "C1" to listOf(
                    SlackConversationHistoryResult.Ok(
                        SlackConversationHistoryPage(
                            messageTimestamps = listOf("1.0"),
                            hasMore = false,
                            nextCursor = null
                        )
                    )
                )
            )
        )
        val service = DefaultSlackChannelAnalysisService(
            pluginManagementService = pluginManagementService,
            activityRepository = activityRepository,
            slackWebApiClient = client
        )
        service.runIncrementalAnalysis(SlackAnalysisTrigger.SCHEDULED) { }
        val logged = activityRepository.findByType(ActivityType.SLACK_ANALYSIS_RUN)
        assertEquals(1, logged.size)
        assertEquals(ActivityStatus.SUCCESS, logged.single().status)
    }
}
