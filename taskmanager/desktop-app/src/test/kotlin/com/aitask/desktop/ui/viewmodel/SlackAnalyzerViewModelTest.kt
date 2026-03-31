package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryActivityRepository
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

private class FakeSlackChannelAnalysisService : SlackChannelAnalysisService {
    override suspend fun runIncrementalAnalysis(
        trigger: SlackAnalysisTrigger,
        onProgress: (String) -> Unit
    ): Result<SlackAnalysisRunResult> {
        onProgress("Loading configuration…")
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
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = InMemoryActivityRepository()
            ),
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
    fun `runAnalysis updates progress then summary without error`() = runTest(testDispatcher) {
        viewModel = SlackAnalyzerViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = InMemoryActivityRepository()
            ),
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
}
