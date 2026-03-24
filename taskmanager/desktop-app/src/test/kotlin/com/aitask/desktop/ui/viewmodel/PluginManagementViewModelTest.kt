package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryActivityRepository
import com.aitask.core.infrastructure.plugin.InMemoryPluginManagementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PluginManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PluginManagementViewModel

    @BeforeEach
    fun setUp() {
        viewModel = PluginManagementViewModel(
            pluginManagementService = InMemoryPluginManagementService(
                activityRepository = InMemoryActivityRepository()
            ),
            scope = CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `loadCatalog populates plugins and core features`() = runTest(testDispatcher) {
        viewModel.loadCatalog()
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.coreFeatures.size)
        assertEquals(3, viewModel.uiState.plugins.size)
        assertFalse(viewModel.uiState.isLoading)
        assertTrue(viewModel.uiState.feedback == null)
    }

    @Test
    fun `install action refreshes catalog and shows feedback`() = runTest(testDispatcher) {
        viewModel.loadCatalog()
        advanceUntilIdle()

        viewModel.install("plugin.release-notes")
        advanceUntilIdle()

        val installedPlugin = viewModel.uiState.plugins.first { it.id == "plugin.release-notes" }
        assertTrue(installedPlugin.installed)
        assertTrue(viewModel.uiState.feedback?.message?.contains("installed successfully") == true)
        assertFalse(viewModel.uiState.feedback?.isError == true)
    }

    @Test
    fun `configuration editor validates and saves plugin settings`() = runTest(testDispatcher) {
        viewModel.loadCatalog()
        advanceUntilIdle()

        viewModel.openConfigurationEditor("plugin.code-review-assistant")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.configurationEditor)
        viewModel.updateConfigurationField("review_summary_prefix", "Review")
        viewModel.updateConfigurationSecret("review_token", "secret-token")
        viewModel.validateConfiguration()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.configurationEditor?.validationResult?.valid == true)

        viewModel.saveConfiguration()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.feedback?.message?.contains("configuration saved") == true)
        assertTrue(
            viewModel.uiState.configurationEditor?.validationResult?.valid == true
        )
    }
}
