package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryLlmConfigurationRepository
import com.aitask.core.domain.model.GeppaConnectionTestResult
import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.McpServerConfigurationRequest
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.repository.GeppaConfigurationRepository
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import com.aitask.core.domain.model.LlmConnectionTestResult
import com.aitask.core.domain.service.GeppaConnectionValidator
import com.aitask.core.domain.service.McpServerManifest
import com.aitask.core.domain.service.McpBridgeService
import com.aitask.core.domain.service.LlmConnectionValidator
import com.aitask.core.domain.usecase.DeleteAgentDefinitionUseCase
import com.aitask.core.domain.usecase.GetProjectsUseCase
import com.aitask.core.domain.usecase.SaveAgentDefinitionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var getProjectsUseCase: GetProjectsUseCase
    private lateinit var agentDefinitionRepository: AgentDefinitionRepository
    private lateinit var mcpServerConfigurationRepository: McpServerConfigurationRepository
    private lateinit var mcpBridgeService: McpBridgeService
    private lateinit var geppaConfigurationRepository: GeppaConfigurationRepository
    private lateinit var geppaConnectionValidator: GeppaConnectionValidator
    private lateinit var viewModel: SettingsViewModel

    @BeforeEach
    fun setUp() {
        repository = InMemoryLlmConfigurationRepository()
        getProjectsUseCase = mockk()
        agentDefinitionRepository = mockk()
        mcpServerConfigurationRepository = mockk()
        mcpBridgeService = mockk()
        geppaConfigurationRepository = mockk()
        geppaConnectionValidator = mockk()
        coEvery { getProjectsUseCase(includeArchived = false) } returns Result.success(emptyList())
        coEvery { agentDefinitionRepository.findAll() } returns emptyList()
        coEvery { mcpServerConfigurationRepository.find() } returns null
        coEvery { mcpServerConfigurationRepository.save(any()) } answers {
            val request = arg<McpServerConfigurationRequest>(0)
            McpServerConfiguration(
                id = request.id ?: java.util.UUID.randomUUID(),
                isEnabled = request.isEnabled,
                port = request.port,
                createdAt = java.time.Instant.now(),
                updatedAt = java.time.Instant.now()
            )
        }
        every { mcpBridgeService.currentManifest() } returns McpServerManifest(
            serverName = "AiTask MCP bridge",
            endpointUrl = "http://127.0.0.1:3333/mcp",
            capabilities = listOf("task-context", "project-context", "repository-context"),
            tools = emptyList(),
            resources = emptyList(),
            safetyNotes = listOf("MCP bridge disabled")
        )
        coEvery { mcpBridgeService.sync() } returns mcpBridgeService.currentManifest()
        coEvery { mcpBridgeService.stop() } returns mcpBridgeService.currentManifest()
        coEvery { geppaConfigurationRepository.find() } returns null
        viewModel = SettingsViewModel(
            getProjectsUseCase = getProjectsUseCase,
            llmConfigurationRepository = repository,
            agentDefinitionRepository = agentDefinitionRepository,
            mcpServerConfigurationRepository = mcpServerConfigurationRepository,
            mcpBridgeService = mcpBridgeService,
            saveAgentDefinitionUseCase = mockk<SaveAgentDefinitionUseCase>(relaxed = true),
            deleteAgentDefinitionUseCase = mockk<DeleteAgentDefinitionUseCase>(relaxed = true),
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
            geppaConfigurationRepository = geppaConfigurationRepository,
            geppaConnectionValidator = geppaConnectionValidator,
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
            getProjectsUseCase = getProjectsUseCase,
            llmConfigurationRepository = repository,
            agentDefinitionRepository = agentDefinitionRepository,
            mcpServerConfigurationRepository = mcpServerConfigurationRepository,
            mcpBridgeService = mcpBridgeService,
            saveAgentDefinitionUseCase = mockk(relaxed = true),
            deleteAgentDefinitionUseCase = mockk(relaxed = true),
            llmConnectionValidator = object : LlmConnectionValidator {
                override suspend fun validate(endpointUrl: String, apiKey: String?): Result<LlmConnectionTestResult> =
                    Result.failure(IllegalStateException("Endpoint unreachable"))
            },
            geppaConfigurationRepository = geppaConfigurationRepository,
            geppaConnectionValidator = geppaConnectionValidator,
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
    fun `saveMcpConfiguration persists bridge settings and syncs service`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateMcpEditorEnabled(true)
        viewModel.updateMcpEditorPort("4011")
        viewModel.saveMcpConfiguration()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isMcpSaving)
        assertEquals(4011, viewModel.uiState.mcpEditor.port)
        assertTrue(viewModel.uiState.mcpFeedback?.contains("http://127.0.0.1:4011/mcp") == true)
        coVerify { mcpServerConfigurationRepository.save(match { it.isEnabled && it.port == 4011 }) }
        coVerify { mcpBridgeService.sync() }
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
    fun `local validation prevents blank agent fields before save`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.startNewAgentDefinition()
        viewModel.updateAgentEditorScope(AgentScope.GLOBAL)
        viewModel.saveAgentDefinition()

        advanceUntilIdle()

        assertEquals("Enter an agent name", viewModel.uiState.agentError)
        assertFalse(viewModel.uiState.isAgentSaving)
    }

    @Test
    fun `saveGeppaConfiguration persists when disabled without validating connection`() = runTest(testDispatcher) {
        val savedConfig = com.aitask.core.domain.model.GeppaConfiguration(
            id = java.util.UUID.randomUUID(),
            isEnabled = false,
            endpointUrl = "",
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        coEvery { geppaConfigurationRepository.save(any()) } returns savedConfig
        advanceUntilIdle()

        viewModel.updateGeppaEditorEnabled(false)
        viewModel.saveGeppaConfiguration()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeppaSaving)
        assertEquals("GEPPA integration disabled", viewModel.uiState.geppaFeedback)
        coVerify(exactly = 0) { geppaConnectionValidator.validate(any()) }
        coVerify { geppaConfigurationRepository.save(match { !it.isEnabled }) }
    }

    @Test
    fun `saveGeppaConfiguration validates connection when enabled`() = runTest(testDispatcher) {
        val savedConfig = com.aitask.core.domain.model.GeppaConfiguration(
            id = java.util.UUID.randomUUID(),
            isEnabled = true,
            endpointUrl = "http://localhost:8080",
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        coEvery { geppaConnectionValidator.validate(any()) } returns Result.success(
            GeppaConnectionTestResult(probeUrl = "http://localhost:8080/health", statusCode = 200, latencyMillis = 5)
        )
        coEvery { geppaConfigurationRepository.save(any()) } returns savedConfig
        advanceUntilIdle()

        viewModel.updateGeppaEditorEnabled(true)
        viewModel.updateGeppaEditorEndpoint("http://localhost:8080")
        viewModel.saveGeppaConfiguration()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeppaSaving)
        assertEquals("GEPPA integration enabled", viewModel.uiState.geppaFeedback)
        coVerify { geppaConnectionValidator.validate("http://localhost:8080") }
    }

    @Test
    fun `saveGeppaConfiguration shows error when enabled but connection fails`() = runTest(testDispatcher) {
        coEvery { geppaConnectionValidator.validate(any()) } returns Result.failure(
            IllegalStateException("Unable to reach the GEPPA endpoint")
        )
        advanceUntilIdle()

        viewModel.updateGeppaEditorEnabled(true)
        viewModel.updateGeppaEditorEndpoint("http://localhost:9999")
        viewModel.saveGeppaConfiguration()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeppaSaving)
        assertEquals("Unable to reach the GEPPA endpoint", viewModel.uiState.geppaError)
        coVerify(exactly = 0) { geppaConfigurationRepository.save(any()) }
    }

    @Test
    fun `saveGeppaConfiguration rejects blank endpoint when enabled`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateGeppaEditorEnabled(true)
        viewModel.saveGeppaConfiguration()

        assertEquals("Enter the GEPPA endpoint URL", viewModel.uiState.geppaError)
        assertFalse(viewModel.uiState.isGeppaSaving)
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
