package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.service.SlackAnalysisChannelProgress
import com.aitask.core.domain.service.SlackAnalysisRunResult
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.core.domain.service.SlackChannelAnalysisService
import com.aitask.core.domain.service.SlackChannelRunOutcome
import com.aitask.core.infrastructure.plugin.InMemoryPluginManagementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private class FakeSlackChannelAnalysisService : SlackChannelAnalysisService {
    override suspend fun runIncrementalAnalysis(
        trigger: SlackAnalysisTrigger,
        onProgress: (String) -> Unit,
        onChannelProgress: (SlackAnalysisChannelProgress) -> Unit
    ): Result<SlackAnalysisRunResult> {
        onProgress("Loading configuration…")
        onChannelProgress(SlackAnalysisChannelProgress(channelBeingProcessed = "C1", pendingChannelIds = emptyList()))
        onProgress("Analyzing channel C1…")
        return Result.success(
            SlackAnalysisRunResult(
                outcomes = listOf(
                    SlackChannelRunOutcome.Processed(
                        channelId = "C1",
                        messageCount = 1,
                        summaries = emptyList()
                    )
                ),
                summaryMessage = "Slack analysis finished: processed=1, skipped (no new messages)=0, channel failures=0, topic summaries=0."
            )
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SlackAnalyzerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SlackAnalyzerViewModel

    @BeforeEach
    fun setUp() {
        val activityRepo = InMemoryActivityRepository()
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            scope = CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `load exposes slack analyzer plugin configuration`() = runTest(testDispatcher) {
        viewModel.load()
        advanceUntilIdle()

        assertNull(viewModel.uiState.error)
        assertNotNull(viewModel.uiState.editor)
        assertEquals(SlackAnalyzerViewModel.PLUGIN_ID, viewModel.uiState.editor?.plugin?.id)
        assertTrue(viewModel.uiState.isLoading == false)
    }

    @Test
    fun `load populates slack analysis run history`() = runTest(testDispatcher) {
        val activityRepo = InMemoryActivityRepository()
        activityRepo.create(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.SLACK_ANALYSIS_RUN,
                entityType = "plugin",
                entityId = UUID.randomUUID(),
                description = "Slack analysis finished: processed=1, skipped (no new messages)=0, channel failures=0, topic summaries=0.",
                metadata = mapOf("trigger" to "MANUAL", "processedCount" to "1"),
                createdAt = Instant.now(),
                status = ActivityStatus.SUCCESS
            )
        )
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            scope = CoroutineScope(testDispatcher)
        )
        viewModel.load()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.runHistory.size)
        assertEquals(ActivityType.SLACK_ANALYSIS_RUN, viewModel.uiState.runHistory.single().type)
    }

    @Test
    fun `runAnalysis updates progress then summary without error`() = runTest(testDispatcher) {
        val activityRepo = InMemoryActivityRepository()
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            slackChannelAnalysisService = FakeSlackChannelAnalysisService(),
            scope = CoroutineScope(testDispatcher)
        )
        viewModel.load()
        advanceUntilIdle()
        viewModel.runAnalysis()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.analysisRunInProgress)
        assertEquals(
            "Slack analysis finished: processed=1, skipped (no new messages)=0, channel failures=0, topic summaries=0.",
            viewModel.uiState.analysisLastSummary
        )
        assertNull(viewModel.uiState.analysisLastError)
    }

    @Test
    fun `run history filters by ActivityStatus`() = runTest(testDispatcher) {
        val activityRepo = InMemoryActivityRepository()
        val okId: UUID = UUID.randomUUID()
        val failId: UUID = UUID.randomUUID()
        activityRepo.create(
            slackRunActivity(
                id = okId,
                status = ActivityStatus.SUCCESS,
                trigger = SlackAnalysisTrigger.MANUAL,
                createdAt = fixedInstant(year = 2024, month = 6, day = 1, hour = 10)
            )
        )
        activityRepo.create(
            slackRunActivity(
                id = failId,
                status = ActivityStatus.FAILED,
                trigger = SlackAnalysisTrigger.MANUAL,
                createdAt = fixedInstant(year = 2024, month = 6, day = 1, hour = 11)
            )
        )
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            scope = CoroutineScope(testDispatcher)
        )
        viewModel.load()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.runHistory.size)
        viewModel.setRunHistoryStatusFilter(ActivityStatus.FAILED)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.runHistory.size)
        assertEquals(failId, viewModel.uiState.runHistory.single().id)
        viewModel.clearRunHistoryFilters()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.runHistory.size)
    }

    @Test
    fun `run history filters by SlackAnalysisTrigger metadata`() = runTest(testDispatcher) {
        val activityRepo = InMemoryActivityRepository()
        val manualId: UUID = UUID.randomUUID()
        val schedId: UUID = UUID.randomUUID()
        activityRepo.create(
            slackRunActivity(
                id = manualId,
                status = ActivityStatus.SUCCESS,
                trigger = SlackAnalysisTrigger.MANUAL,
                createdAt = fixedInstant(year = 2024, month = 7, day = 1, hour = 9)
            )
        )
        activityRepo.create(
            slackRunActivity(
                id = schedId,
                status = ActivityStatus.SUCCESS,
                trigger = SlackAnalysisTrigger.SCHEDULED,
                createdAt = fixedInstant(year = 2024, month = 7, day = 1, hour = 10)
            )
        )
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            scope = CoroutineScope(testDispatcher)
        )
        viewModel.load()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.runHistory.size)
        viewModel.setRunHistoryTriggerFilter(SlackAnalysisTrigger.SCHEDULED)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.runHistory.size)
        assertEquals(schedId, viewModel.uiState.runHistory.single().id)
        assertEquals("SCHEDULED", viewModel.uiState.runHistory.single().metadata["trigger"])
    }

    @Test
    fun `run history filters by local date range`() = runTest(testDispatcher) {
        val activityRepo = InMemoryActivityRepository()
        val day1Id: UUID = UUID.randomUUID()
        val day2Id: UUID = UUID.randomUUID()
        activityRepo.create(
            slackRunActivity(
                id = day1Id,
                status = ActivityStatus.SUCCESS,
                trigger = SlackAnalysisTrigger.MANUAL,
                createdAt = fixedInstant(year = 2025, month = 3, day = 10, hour = 8)
            )
        )
        activityRepo.create(
            slackRunActivity(
                id = day2Id,
                status = ActivityStatus.SUCCESS,
                trigger = SlackAnalysisTrigger.MANUAL,
                createdAt = fixedInstant(year = 2025, month = 3, day = 20, hour = 8)
            )
        )
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = activityRepo
            ),
            activityRepository = activityRepo,
            scope = CoroutineScope(testDispatcher)
        )
        viewModel.load()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.runHistory.size)
        val fromStr: String = LocalDate.of(2025, 3, 10).toString()
        val toStr: String = LocalDate.of(2025, 3, 10).toString()
        viewModel.setRunHistoryDateFrom(fromStr)
        advanceUntilIdle()
        viewModel.setRunHistoryDateTo(toStr)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.runHistory.size)
        assertEquals(day1Id, viewModel.uiState.runHistory.single().id)
    }
}

private fun slackRunActivity(
    id: UUID,
    status: ActivityStatus,
    trigger: SlackAnalysisTrigger,
    createdAt: Instant
): Activity =
    Activity(
        id = id,
        type = ActivityType.SLACK_ANALYSIS_RUN,
        entityType = "plugin",
        entityId = UUID.randomUUID(),
        description = "Slack analysis finished",
        metadata = mapOf("trigger" to trigger.name),
        createdAt = createdAt,
        status = status
    )

private fun fixedInstant(year: Int, month: Int, day: Int, hour: Int): Instant {
    val zone: ZoneId = ZoneId.systemDefault()
    return LocalDate.of(year, month, day).atTime(hour, 0).atZone(zone).toInstant()
}
