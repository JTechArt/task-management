package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.infrastructure.plugin.InMemoryPluginManagementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
}
