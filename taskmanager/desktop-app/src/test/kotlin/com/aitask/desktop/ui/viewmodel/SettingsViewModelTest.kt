package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryLlmConfigurationRepository
import com.aitask.core.domain.model.LlmConnectionTestResult
import com.aitask.core.domain.service.LlmConnectionValidator
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryLlmConfigurationRepository
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        repository = InMemoryLlmConfigurationRepository()
        viewModel = SettingsViewModel(
            llmConfigurationRepository = repository,
            llmConnectionValidator = object : LlmConnectionValidator {
                override suspend fun validate(endpointUrl: String, apiKey: String?): Result<LlmConnectionTestResult> =
                    Result.success(
                        LlmConnectionTestResult(
                            probeUrl = endpointUrl,
                            statusCode = 200,
                            latencyMillis = 12
                        )
                    )
            },
            scope = CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `saveLlmConfiguration validates and persists a local profile`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateLlmEditorName("Local Ollama")
        viewModel.updateLlmEditorEndpoint("http://localhost:11434")
        viewModel.updateLlmEditorModel("llama3.1")
        viewModel.updateLlmEditorApiKey("secret-token")
        viewModel.updateLlmEditorDefault(true)
        viewModel.saveLlmConfiguration()

        advanceUntilIdle()

        assertEquals(1, repository.findAll().size)
        val saved = repository.findAll().first()
        assertTrue(saved.isDefault)
        assertTrue(saved.hasApiKey)
        assertEquals("Local Ollama", saved.name)
        assertEquals("http://localhost:11434", saved.endpointUrl)
        assertNull(viewModel.uiState.llmEditor.id)
        assertNotNull(viewModel.uiState.llmFeedback)
        assertFalse(viewModel.uiState.isLlmSaving)
    }

    @Test
    fun `saveLlmConfiguration does not persist when validation fails`() = runTest(testDispatcher) {
        viewModel = SettingsViewModel(
            llmConfigurationRepository = repository,
            llmConnectionValidator = object : LlmConnectionValidator {
                override suspend fun validate(endpointUrl: String, apiKey: String?): Result<LlmConnectionTestResult> =
                    Result.failure(IllegalStateException("Endpoint unreachable"))
            },
            scope = CoroutineScope(testDispatcher)
        )
        advanceUntilIdle()

        viewModel.updateLlmEditorName("Broken")
        viewModel.updateLlmEditorEndpoint("http://localhost:9")
        viewModel.updateLlmEditorModel("llama3.1")
        viewModel.saveLlmConfiguration()

        advanceUntilIdle()

        assertTrue(repository.findAll().isEmpty())
        assertEquals("Endpoint unreachable", viewModel.uiState.llmError)
        assertFalse(viewModel.uiState.isLlmSaving)
    }

    @Test
    fun `local validation prevents blank llm profile fields before connection test`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.startNewLlmConfiguration()
        viewModel.testLlmConfigurationConnection()

        assertEquals("Enter a display name for the LLM configuration", viewModel.uiState.llmError)
        assertFalse(viewModel.uiState.isLlmTesting)
    }

    @Test
    fun `setDefaultLlmConfiguration moves the default flag`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateLlmEditorName("First")
        viewModel.updateLlmEditorEndpoint("http://localhost:11434")
        viewModel.updateLlmEditorModel("llama3.1")
        viewModel.updateLlmEditorDefault(true)
        viewModel.saveLlmConfiguration()
        advanceUntilIdle()

        viewModel.startNewLlmConfiguration()
        viewModel.updateLlmEditorName("Second")
        viewModel.updateLlmEditorEndpoint("http://localhost:11435")
        viewModel.updateLlmEditorModel("qwen2.5")
        viewModel.saveLlmConfiguration()
        advanceUntilIdle()

        val second = repository.findAll().first { it.name == "Second" }
        viewModel.setDefaultLlmConfiguration(second.id)
        advanceUntilIdle()

        val refreshed = repository.findAll()
        assertTrue(refreshed.first { it.name == "Second" }.isDefault)
        assertFalse(refreshed.first { it.name == "First" }.isDefault)
    }
}
