package com.aitask.desktop.ui.viewmodel

import com.aitask.core.data.repository.InMemoryLlmConfigurationRepository
import com.aitask.core.domain.model.ClaudeConfiguration
import com.aitask.core.domain.model.ClaudeIntegrationMode
import com.aitask.core.domain.model.CodexConfiguration
import com.aitask.core.domain.model.GeppaConnectionTestResult
import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.McpServerConfigurationRequest
import com.aitask.core.domain.model.SavedPrompt
import com.aitask.core.domain.model.SavedPromptRequest
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.repository.ClaudeConfigurationRepository
import com.aitask.core.domain.repository.CodexConfigurationRepository
import com.aitask.core.domain.repository.GeppaConfigurationRepository
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import com.aitask.core.domain.repository.SavedPromptRepository
import com.aitask.core.domain.model.LlmConnectionTestResult
import com.aitask.core.domain.service.ClaudeAnthropicApiService
import com.aitask.core.domain.service.ClaudeCliService
import com.aitask.core.domain.service.CodexCliService
import com.aitask.core.domain.service.GeppaConnectionValidator
import com.aitask.core.domain.service.McpServerManifest
import com.aitask.core.domain.service.McpBridgeService
import com.aitask.core.domain.service.LlmConnectionValidator
import com.aitask.core.domain.usecase.DeleteAgentDefinitionUseCase
import com.aitask.core.domain.usecase.DeleteSavedPromptUseCase
import com.aitask.core.domain.usecase.GetProjectsUseCase
import com.aitask.core.domain.usecase.SaveAgentDefinitionUseCase
import com.aitask.core.domain.usecase.SaveSavedPromptUseCase
import io.mockk.*
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
    private lateinit var codexConfigurationRepository: CodexConfigurationRepository
    private lateinit var codexCliService: CodexCliService
    private lateinit var claudeConfigurationRepository: ClaudeConfigurationRepository
    private lateinit var claudeCliService: ClaudeCliService
    private lateinit var claudeAnthropicApiService: ClaudeAnthropicApiService
    private lateinit var savedPromptRepository: SavedPromptRepository
    private lateinit var saveSavedPromptUseCase: SaveSavedPromptUseCase
    private lateinit var deleteSavedPromptUseCase: DeleteSavedPromptUseCase
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
        codexConfigurationRepository = mockk(relaxed = true)
        codexCliService = mockk(relaxed = true)
        claudeConfigurationRepository = mockk(relaxed = true)
        claudeCliService = mockk(relaxed = true)
        claudeAnthropicApiService = mockk(relaxed = true)
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
        coEvery { codexConfigurationRepository.find() } returns null
        coEvery { claudeConfigurationRepository.find() } returns null
        savedPromptRepository = mockk<SavedPromptRepository>()
        coEvery { savedPromptRepository.findAll() } returns emptyList()
        saveSavedPromptUseCase = mockk<SaveSavedPromptUseCase>(relaxed = true)
        deleteSavedPromptUseCase = mockk<DeleteSavedPromptUseCase>(relaxed = true)
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
            codexConfigurationRepository = codexConfigurationRepository,
            codexCliService = codexCliService,
            claudeConfigurationRepository = claudeConfigurationRepository,
            claudeCliService = claudeCliService,
            claudeAnthropicApiService = claudeAnthropicApiService,
            savedPromptRepository = savedPromptRepository,
            saveSavedPromptUseCase = saveSavedPromptUseCase,
            deleteSavedPromptUseCase = deleteSavedPromptUseCase,
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
            codexConfigurationRepository = codexConfigurationRepository,
            codexCliService = codexCliService,
            claudeConfigurationRepository = claudeConfigurationRepository,
            claudeCliService = claudeCliService,
            claudeAnthropicApiService = claudeAnthropicApiService,
            savedPromptRepository = savedPromptRepository,
            saveSavedPromptUseCase = saveSavedPromptUseCase,
            deleteSavedPromptUseCase = deleteSavedPromptUseCase,
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

    @Test
    fun `saveSavedPrompt persists a new global prompt and shows feedback`() = runTest(testDispatcher) {
        val savedPrompt = SavedPrompt(
            id = java.util.UUID.randomUUID(),
            name = "My Prompt",
            content = "Summarize {{taskTitle}}",
            category = "Summarization",
            scope = AgentScope.GLOBAL,
            projectId = null,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        coEvery { saveSavedPromptUseCase(any<SavedPromptRequest>()) } returns Result.success(savedPrompt)
        coEvery { savedPromptRepository.findAll() } returnsMany listOf(emptyList(), listOf(savedPrompt))
        advanceUntilIdle()

        viewModel.updateSavedPromptEditorName("My Prompt")
        viewModel.updateSavedPromptEditorContent("Summarize {{taskTitle}}")
        viewModel.updateSavedPromptEditorCategory("Summarization")
        viewModel.saveSavedPrompt()
        advanceUntilIdle()

        assertEquals("Prompt saved", viewModel.uiState.savedPromptFeedback)
        assertFalse(viewModel.uiState.isSavedPromptSaving)
        assertNull(viewModel.uiState.savedPromptEditor.id)
    }

    @Test
    fun `saveSavedPrompt rejects blank name`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateSavedPromptEditorContent("Some content")
        viewModel.saveSavedPrompt()

        assertEquals("Enter a prompt name", viewModel.uiState.savedPromptError)
        assertFalse(viewModel.uiState.isSavedPromptSaving)
    }

    @Test
    fun `saveSavedPrompt rejects blank content`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.updateSavedPromptEditorName("My Prompt")
        viewModel.saveSavedPrompt()

        assertEquals("Enter the prompt content", viewModel.uiState.savedPromptError)
        assertFalse(viewModel.uiState.isSavedPromptSaving)
    }

    @Test
    fun `editSavedPrompt populates editor fields`() = runTest(testDispatcher) {
        val promptId = java.util.UUID.randomUUID()
        val prompt = SavedPrompt(
            id = promptId,
            name = "Existing Prompt",
            content = "Existing content",
            category = "Category",
            scope = AgentScope.GLOBAL,
            projectId = null,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        advanceUntilIdle()

        viewModel.editSavedPrompt(prompt)

        assertEquals(promptId, viewModel.uiState.savedPromptEditor.id)
        assertEquals("Existing Prompt", viewModel.uiState.savedPromptEditor.name)
        assertEquals("Existing content", viewModel.uiState.savedPromptEditor.content)
        assertEquals("Category", viewModel.uiState.savedPromptEditor.category)
    }

    @Test
    fun `deleteSavedPrompt removes prompt and shows feedback`() = runTest(testDispatcher) {
        val promptId = java.util.UUID.randomUUID()
        coEvery { deleteSavedPromptUseCase(promptId) } returns Result.success(true)
        coEvery { savedPromptRepository.findAll() } returnsMany listOf(emptyList(), emptyList())
        advanceUntilIdle()

        viewModel.deleteSavedPrompt(promptId)
        advanceUntilIdle()

        assertEquals("Prompt deleted", viewModel.uiState.savedPromptFeedback)
    }

    @Test
    fun `applyPromptFromLibrary copies content into agent editor`() = runTest(testDispatcher) {
        val prompt = SavedPrompt(
            id = java.util.UUID.randomUUID(),
            name = "Library Prompt",
            content = "Summarize {{taskTitle}} for {{projectName}}",
            category = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            createdAt = java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
        advanceUntilIdle()

        viewModel.applyPromptFromLibrary(prompt)

        assertEquals("Summarize {{taskTitle}} for {{projectName}}", viewModel.uiState.agentEditor.promptTemplate)
    }

    @Test
    fun `testCodexCli shows feedback when cli resolves and validates`() = runTest(testDispatcher) {
        coEvery { codexCliService.resolveExecutable(any()) } returns Result.success("/opt/codex")
        coEvery { codexCliService.validateExecutable("/opt/codex") } returns Result.success(Unit)
        advanceUntilIdle()
        viewModel.updateCodexEditorEnabled(true)
        viewModel.testCodexCli()
        advanceUntilIdle()
        assertEquals("Codex CLI is available at /opt/codex", viewModel.uiState.codexFeedback)
        assertFalse(viewModel.uiState.isCodexTesting)
    }

    @Test
    fun `testCodexCli shows error when resolve fails`() = runTest(testDispatcher) {
        coEvery { codexCliService.resolveExecutable(any()) } returns Result.failure(IllegalStateException("not found"))
        advanceUntilIdle()
        viewModel.updateCodexEditorEnabled(true)
        viewModel.testCodexCli()
        advanceUntilIdle()
        assertEquals("not found", viewModel.uiState.codexError)
    }

    @Test
    fun `saveCodexConfiguration when enabled validates and persists`() = runTest(testDispatcher) {
        val id = java.util.UUID.randomUUID()
        coEvery { codexCliService.resolveExecutable(any()) } returns Result.success("/bin/codex")
        coEvery { codexCliService.validateExecutable("/bin/codex") } returns Result.success(Unit)
        coEvery { codexConfigurationRepository.save(any()) } returns CodexConfiguration(
            id = id,
            isEnabled = true,
            cliPath = "/bin/codex",
            hasApiKey = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        advanceUntilIdle()
        viewModel.updateCodexEditorEnabled(true)
        viewModel.updateCodexEditorCliPath("/bin/codex")
        viewModel.saveCodexConfiguration()
        advanceUntilIdle()
        assertEquals("Codex CLI settings saved", viewModel.uiState.codexFeedback)
        assertFalse(viewModel.uiState.isCodexSaving)
        coVerify(atLeast = 1) { codexConfigurationRepository.save(match { it.isEnabled && it.cliPath == "/bin/codex" }) }
    }

    @Test
    fun `saveCodexConfiguration when enabled stops on resolve failure`() = runTest(testDispatcher) {
        coEvery { codexCliService.resolveExecutable(any()) } returns Result.failure(IllegalStateException("bad path"))
        advanceUntilIdle()
        viewModel.updateCodexEditorEnabled(true)
        viewModel.saveCodexConfiguration()
        advanceUntilIdle()
        assertEquals("bad path", viewModel.uiState.codexError)
        coVerify(exactly = 0) { codexConfigurationRepository.save(any()) }
    }

    @Test
    fun `testClaudeIntegration CLI shows feedback when cli resolves and validates`() = runTest(testDispatcher) {
        coEvery { claudeCliService.resolveExecutable(any()) } returns Result.success("/opt/claude")
        coEvery { claudeCliService.validateExecutable("/opt/claude") } returns Result.success(Unit)
        advanceUntilIdle()
        viewModel.updateClaudeEditorEnabled(true)
        viewModel.updateClaudeEditorIntegrationMode(ClaudeIntegrationMode.CLI)
        viewModel.testClaudeIntegration()
        advanceUntilIdle()
        assertEquals("Claude CLI is available at /opt/claude", viewModel.uiState.claudeFeedback)
        assertFalse(viewModel.uiState.isClaudeTesting)
    }

    @Test
    fun `saveClaudeConfiguration API mode validates and persists`() = runTest(testDispatcher) {
        val id = java.util.UUID.randomUUID()
        coEvery { claudeAnthropicApiService.validateApiKey(any(), any()) } returns Result.success(Unit)
        coEvery { claudeConfigurationRepository.save(any()) } returns ClaudeConfiguration(
            id = id,
            isEnabled = true,
            integrationMode = ClaudeIntegrationMode.API,
            cliPath = "",
            apiBaseUrl = null,
            hasApiKey = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        advanceUntilIdle()
        viewModel.updateClaudeEditorEnabled(true)
        viewModel.updateClaudeEditorIntegrationMode(ClaudeIntegrationMode.API)
        viewModel.updateClaudeEditorApiKey("sk-ant-test")
        viewModel.saveClaudeConfiguration()
        advanceUntilIdle()
        assertEquals("Claude API settings saved", viewModel.uiState.claudeFeedback)
        assertFalse(viewModel.uiState.isClaudeSaving)
        coVerify(atLeast = 1) {
            claudeConfigurationRepository.save(
                match { it.isEnabled && it.integrationMode == ClaudeIntegrationMode.API }
            )
        }
    }
}
