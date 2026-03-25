package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentDefinitionRequest
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.AgentTrigger
import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.model.McpServerConfigurationRequest
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.LlmConfigurationRequest
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.usecase.DeleteAgentDefinitionUseCase
import com.aitask.core.domain.service.LlmConnectionValidator
import com.aitask.core.domain.service.McpBridgeService
import com.aitask.core.domain.usecase.CreateBackupUseCase
import com.aitask.core.domain.usecase.ExportDataUseCase
import com.aitask.core.domain.usecase.ImportDataUseCase
import com.aitask.core.domain.usecase.GetProjectsUseCase
import com.aitask.core.domain.usecase.SaveAgentDefinitionUseCase
import com.aitask.core.domain.usecase.RestoreFromBackupUseCase
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase = DependencyContainer.exportDataUseCase,
    private val importDataUseCase: ImportDataUseCase = DependencyContainer.importDataUseCase,
    private val createBackupUseCase: CreateBackupUseCase = DependencyContainer.createBackupUseCase,
    private val restoreFromBackupUseCase: RestoreFromBackupUseCase = DependencyContainer.restoreFromBackupUseCase,
    private val getProjectsUseCase: GetProjectsUseCase = DependencyContainer.getProjectsUseCase,
    private val llmConfigurationRepository: LlmConfigurationRepository = DependencyContainer.llmConfigurationRepository,
    private val agentDefinitionRepository: AgentDefinitionRepository = DependencyContainer.agentDefinitionRepository,
    private val mcpServerConfigurationRepository: McpServerConfigurationRepository = DependencyContainer.mcpServerConfigurationRepository,
    private val mcpBridgeService: McpBridgeService = DependencyContainer.mcpBridgeService,
    private val saveAgentDefinitionUseCase: SaveAgentDefinitionUseCase = DependencyContainer.saveAgentDefinitionUseCase,
    private val deleteAgentDefinitionUseCase: DeleteAgentDefinitionUseCase = DependencyContainer.deleteAgentDefinitionUseCase,
    private val llmConnectionValidator: LlmConnectionValidator = DependencyContainer.llmConnectionValidator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        loadProjects()
        loadLlmConfigurations()
        loadAgentDefinitions()
        loadMcpConfiguration()
    }

    fun loadProjects() {
        scope.launch {
            try {
                val result = getProjectsUseCase(includeArchived = false)
                result.fold(
                    onSuccess = { projects ->
                        uiState = uiState.copy(projects = projects)
                    },
                    onFailure = {
                        uiState = uiState.copy(projects = emptyList())
                    }
                )
            } catch (_: Exception) {
                uiState = uiState.copy(projects = emptyList())
            }
        }
    }

    fun loadLlmConfigurations() {
        scope.launch {
            uiState = uiState.copy(isLlmLoading = true, llmError = null)
            try {
                val configurations = llmConfigurationRepository.findAll()
                uiState = uiState.copy(
                    isLlmLoading = false,
                    llmConfigurations = configurations
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isLlmLoading = false,
                    llmError = error.message ?: "Failed to load LLM configurations"
                )
            }
        }
    }

    fun loadAgentDefinitions() {
        scope.launch {
            uiState = uiState.copy(isAgentLoading = true, agentError = null)
            try {
                val definitions = agentDefinitionRepository.findAll()
                uiState = uiState.copy(
                    isAgentLoading = false,
                    agentDefinitions = definitions
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isAgentLoading = false,
                    agentError = error.message ?: "Failed to load agent definitions"
                )
            }
        }
    }

    fun loadMcpConfiguration() {
        scope.launch {
            uiState = uiState.copy(isMcpLoading = true, mcpError = null)
            try {
                val configuration = mcpServerConfigurationRepository.find()
                val editor = configuration?.let {
                    McpServerEditorState(
                        id = it.id,
                        isEnabled = it.isEnabled,
                        port = it.port
                    )
                } ?: McpServerEditorState()
                uiState = uiState.copy(
                    isMcpLoading = false,
                    mcpEditor = editor,
                    mcpManifest = mcpBridgeService.currentManifest()
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isMcpLoading = false,
                    mcpError = error.message ?: "Failed to load MCP configuration"
                )
            }
        }
    }

    fun startNewLlmConfiguration() {
        uiState = uiState.copy(
            llmEditor = LlmConfigurationEditorState(),
            llmError = null,
            llmFeedback = null
        )
    }

    fun startNewAgentDefinition() {
        uiState = uiState.copy(
            agentEditor = AgentDefinitionEditorState(),
            agentError = null,
            agentFeedback = null
        )
    }

    fun editLlmConfiguration(configuration: LlmConfiguration) {
        uiState = uiState.copy(
            llmEditor = LlmConfigurationEditorState(
                id = configuration.id,
                name = configuration.name,
                endpointUrl = configuration.endpointUrl,
                modelIdentifier = configuration.modelIdentifier,
                apiKey = "",
                isDefault = configuration.isDefault
            ),
            llmError = null,
            llmFeedback = null
        )
    }

    fun editAgentDefinition(definition: AgentDefinition) {
        uiState = uiState.copy(
            agentEditor = AgentDefinitionEditorState(
                id = definition.id,
                name = definition.name,
                description = definition.description.orEmpty(),
                promptTemplate = definition.promptTemplate,
                llmConfigurationId = definition.llmConfigurationId,
                scope = definition.scope,
                projectId = definition.projectId,
                trigger = definition.trigger,
                isEnabled = definition.isEnabled
            ),
            agentError = null,
            agentFeedback = null
        )
    }

    fun updateLlmEditorName(name: String) = updateLlmEditor { it.copy(name = name) }

    fun updateLlmEditorEndpoint(endpointUrl: String) = updateLlmEditor { it.copy(endpointUrl = endpointUrl) }

    fun updateLlmEditorModel(modelIdentifier: String) = updateLlmEditor { it.copy(modelIdentifier = modelIdentifier) }

    fun updateLlmEditorApiKey(apiKey: String) = updateLlmEditor { it.copy(apiKey = apiKey) }

    fun updateLlmEditorDefault(isDefault: Boolean) = updateLlmEditor { it.copy(isDefault = isDefault) }

    fun updateAgentEditorName(name: String) = updateAgentEditor { it.copy(name = name) }

    fun updateAgentEditorDescription(description: String) = updateAgentEditor { it.copy(description = description) }

    fun updateAgentEditorPromptTemplate(promptTemplate: String) = updateAgentEditor { it.copy(promptTemplate = promptTemplate) }

    fun updateAgentEditorLlmConfiguration(id: UUID?) = updateAgentEditor { it.copy(llmConfigurationId = id) }

    fun updateAgentEditorScope(scope: AgentScope) = updateAgentEditor { it.copy(scope = scope, projectId = if (scope == AgentScope.GLOBAL) null else it.projectId) }

    fun updateAgentEditorProjectId(projectId: UUID?) = updateAgentEditor { it.copy(projectId = projectId) }

    fun updateAgentEditorTrigger(trigger: AgentTrigger) = updateAgentEditor { it.copy(trigger = trigger) }

    fun updateAgentEditorEnabled(isEnabled: Boolean) = updateAgentEditor { it.copy(isEnabled = isEnabled) }

    fun updateMcpEditorEnabled(isEnabled: Boolean) = updateMcpEditor { it.copy(isEnabled = isEnabled) }

    fun updateMcpEditorPort(port: String) = updateMcpEditor { it.copy(port = port.toIntOrNull() ?: it.port) }

    fun testLlmConfigurationConnection() {
        val editor = uiState.llmEditor
        val error = validateLlmEditorLocally(editor)
        if (error != null) {
            uiState = uiState.copy(llmError = error)
            return
        }
        scope.launch {
            uiState = uiState.copy(isLlmTesting = true, llmError = null, llmFeedback = null)
            val result = llmConnectionValidator.validate(
                endpointUrl = editor.endpointUrl,
                apiKey = editor.apiKey.takeIf { it.isNotBlank() }
            )
            result.fold(
                onSuccess = { testResult ->
                    uiState = uiState.copy(
                        isLlmTesting = false,
                        llmFeedback = "Connection succeeded via ${testResult.probeUrl} in ${testResult.latencyMillis} ms"
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLlmTesting = false,
                        llmError = error.message ?: "LLM connection test failed"
                    )
                }
            )
        }
    }

    fun saveLlmConfiguration() {
        val editor = uiState.llmEditor
        val validationError = validateLlmEditorLocally(editor)
        if (validationError != null) {
            uiState = uiState.copy(llmError = validationError)
            return
        }
        scope.launch {
            uiState = uiState.copy(isLlmSaving = true, llmError = null, llmFeedback = null)
            val validation = llmConnectionValidator.validate(
                endpointUrl = editor.endpointUrl,
                apiKey = editor.apiKey.takeIf { it.isNotBlank() }
            )
            validation.fold(
                onSuccess = {
                    val result = llmConfigurationRepository.save(
                        LlmConfigurationRequest(
                            id = editor.id,
                            name = editor.name,
                            endpointUrl = editor.endpointUrl,
                            modelIdentifier = editor.modelIdentifier,
                            apiKey = editor.apiKey.takeIf { it.isNotBlank() },
                            isDefault = editor.isDefault
                        )
                    )
                    refreshSavedConfiguration(result)
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLlmSaving = false,
                        llmError = error.message ?: "LLM endpoint validation failed"
                    )
                }
            )
        }
    }

    fun saveAgentDefinition() {
        val editor = uiState.agentEditor
        val validationError = validateAgentEditorLocally(editor)
        if (validationError != null) {
            uiState = uiState.copy(agentError = validationError)
            return
        }
        scope.launch {
            uiState = uiState.copy(isAgentSaving = true, agentError = null, agentFeedback = null)
            val result = saveAgentDefinitionUseCase(
                AgentDefinitionRequest(
                    id = editor.id,
                    name = editor.name,
                    description = editor.description.takeIf { it.isNotBlank() },
                    promptTemplate = editor.promptTemplate,
                    llmConfigurationId = editor.llmConfigurationId,
                    scope = editor.scope,
                    projectId = editor.projectId,
                    trigger = editor.trigger,
                    isEnabled = editor.isEnabled
                )
            )
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isAgentSaving = false,
                        agentFeedback = if (editor.id == null) "Agent saved" else "Agent updated",
                        agentEditor = AgentDefinitionEditorState()
                    )
                    loadAgentDefinitions()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isAgentSaving = false,
                        agentError = error.message ?: "Failed to save agent definition"
                    )
                }
            )
        }
    }

    fun saveMcpConfiguration() {
        val editor = uiState.mcpEditor
        val validationError = validateMcpEditorLocally(editor)
        if (validationError != null) {
            uiState = uiState.copy(mcpError = validationError)
            return
        }
        scope.launch {
            uiState = uiState.copy(isMcpSaving = true, mcpError = null, mcpFeedback = null)
            try {
                val saved = mcpServerConfigurationRepository.save(
                    McpServerConfigurationRequest(
                        id = editor.id,
                        isEnabled = editor.isEnabled,
                        port = editor.port
                    )
                )
                val manifest = if (saved.isEnabled) {
                    mcpBridgeService.sync()
                } else {
                    mcpBridgeService.stop()
                }
                uiState = uiState.copy(
                    isMcpSaving = false,
                    mcpEditor = editor.copy(id = saved.id),
                    mcpManifest = manifest,
                    mcpFeedback = if (saved.isEnabled) {
                        "MCP bridge available at ${saved.endpointUrl}"
                    } else {
                        "MCP bridge disabled"
                    }
                )
            } catch (error: Exception) {
                uiState = uiState.copy(
                    isMcpSaving = false,
                    mcpError = error.message ?: "Failed to save MCP configuration"
                )
            }
        }
    }

    fun deleteAgentDefinition(id: UUID) {
        scope.launch {
            uiState = uiState.copy(agentError = null, agentFeedback = null)
            val result = deleteAgentDefinitionUseCase(id)
            if (result.getOrDefault(false)) {
                if (uiState.agentEditor.id == id) {
                    startNewAgentDefinition()
                }
                loadAgentDefinitions()
                uiState = uiState.copy(agentFeedback = "Agent deleted")
            } else {
                uiState = uiState.copy(agentError = "Agent not found")
            }
        }
    }

    fun setDefaultLlmConfiguration(id: UUID) {
        scope.launch {
            uiState = uiState.copy(llmError = null, llmFeedback = null)
            val result = llmConfigurationRepository.setDefault(id)
            if (result == null) {
                uiState = uiState.copy(llmError = "LLM configuration not found")
            } else {
                loadLlmConfigurations()
                if (uiState.llmEditor.id == id) {
                    uiState = uiState.copy(llmEditor = uiState.llmEditor.copy(isDefault = true))
                }
            }
        }
    }

    fun deleteLlmConfiguration(id: UUID) {
        scope.launch {
            uiState = uiState.copy(llmError = null, llmFeedback = null)
            val removed = llmConfigurationRepository.delete(id)
            if (removed) {
                loadLlmConfigurations()
                if (uiState.llmEditor.id == id) {
                    startNewLlmConfiguration()
                }
                uiState = uiState.copy(llmFeedback = "Configuration deleted")
            } else {
                uiState = uiState.copy(llmError = "LLM configuration not found")
            }
        }
    }

    private fun refreshSavedConfiguration(configuration: LlmConfiguration) {
        uiState = uiState.copy(
            isLlmSaving = false,
            llmFeedback = if (configuration.id == uiState.llmEditor.id) {
                "Configuration updated"
            } else {
                "Configuration saved"
            },
            llmEditor = LlmConfigurationEditorState(),
            llmConfigurations = emptyList()
        )
        loadLlmConfigurations()
    }

    private fun validateLlmEditorLocally(editor: LlmConfigurationEditorState): String? = when {
        editor.name.isBlank() -> "Enter a display name for the LLM configuration"
        editor.endpointUrl.isBlank() -> "Enter the endpoint URL for the LLM configuration"
        editor.modelIdentifier.isBlank() -> "Enter the model identifier"
        else -> null
    }

    private fun validateAgentEditorLocally(editor: AgentDefinitionEditorState): String? = when {
        editor.name.isBlank() -> "Enter an agent name"
        editor.promptTemplate.isBlank() -> "Enter a prompt template"
        editor.scope == AgentScope.PROJECT && editor.projectId == null -> "Choose a project for project-scoped agents"
        else -> null
    }

    private fun validateMcpEditorLocally(editor: McpServerEditorState): String? = when {
        editor.port !in 1..65535 -> "Enter a valid MCP port between 1 and 65535"
        else -> null
    }

    private fun updateLlmEditor(transform: (LlmConfigurationEditorState) -> LlmConfigurationEditorState) {
        uiState = uiState.copy(
            llmEditor = transform(uiState.llmEditor),
            llmError = null,
            llmFeedback = null
        )
    }

    private fun updateAgentEditor(transform: (AgentDefinitionEditorState) -> AgentDefinitionEditorState) {
        uiState = uiState.copy(
            agentEditor = transform(uiState.agentEditor),
            agentError = null,
            agentFeedback = null
        )
    }

    private fun updateMcpEditor(transform: (McpServerEditorState) -> McpServerEditorState) {
        uiState = uiState.copy(
            mcpEditor = transform(uiState.mcpEditor),
            mcpError = null,
            mcpFeedback = null
        )
    }

    fun exportData(includeArchived: Boolean = false) {
        scope.launch {
            val file = showSaveFileChooser()
            if (file == null) return@launch
            uiState = uiState.copy(isLoading = true, error = null)
            val result = exportDataUseCase(includeArchived)
            result.fold(
                onSuccess = { json ->
                    runCatching {
                        file.writeText(json)
                    }.fold(
                        onSuccess = {
                            uiState = uiState.copy(
                                isLoading = false,
                                exportFeedback = "Data exported successfully to ${file.name}"
                            )
                        },
                        onFailure = { e ->
                            uiState = uiState.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to write file"
                            )
                        }
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Export failed"
                    )
                }
            )
        }
    }

    fun importData() {
        scope.launch {
            val file = showOpenFileChooser()
            if (file == null) return@launch
            uiState = uiState.copy(isLoading = true, error = null)
            val content = runCatching { file.readText() }.getOrElse {
                uiState = uiState.copy(isLoading = false, error = it.message ?: "Failed to read file")
                return@launch
            }
            val result = importDataUseCase.apply(content)
            result.fold(
                onSuccess = { importResult ->
                    val summary = buildString {
                        append("Import completed: ")
                        append("${importResult.projectsCreated} projects, ")
                        append("${importResult.tasksCreated} tasks, ")
                        append("${importResult.reposCreated} repositories, ")
                        append("${importResult.rulesCreated} rules, ")
                        append("${importResult.projectRulesLinked} rule links")
                        if (importResult.errors.isNotEmpty()) {
                            append(". ${importResult.errors.size} warning(s)")
                        }
                    }
                    uiState = uiState.copy(
                        isLoading = false,
                        importFeedback = summary,
                        error = importResult.errors.firstOrNull()?.takeIf { importResult.errors.size == 1 }
                            ?: if (importResult.errors.size > 1) "${importResult.errors.size} warnings occurred" else null
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Import failed"
                    )
                }
            )
        }
    }

    private suspend fun showSaveFileChooser(backup: Boolean = false): File? = withContext(Dispatchers.Main) {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files (*.json)", "json")
            dialogTitle = if (backup) "Create Backup" else "Export Data"
        }.let { chooser ->
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile.let { f ->
                    when {
                        f.extension.equals("json", ignoreCase = true) -> f
                        else -> File(f.parentFile, "${f.name}.json")
                    }
                }
            } else null
        }
    }

    private suspend fun showOpenFileChooser(): File? = withContext(Dispatchers.Main) {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files (*.json)", "json")
            dialogTitle = "Import Data"
        }.let { chooser ->
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }

    fun clearExportFeedback() {
        uiState = uiState.copy(exportFeedback = null)
    }

    fun clearImportFeedback() {
        uiState = uiState.copy(importFeedback = null)
    }

    fun backupData(includeArchived: Boolean = false) {
        scope.launch {
            val file = showSaveFileChooser(backup = true)
            if (file == null) return@launch
            uiState = uiState.copy(isLoading = true, error = null)
            val result = createBackupUseCase(file, includeArchived)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isLoading = false,
                        exportFeedback = "Backup created successfully: ${file.name}"
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Backup failed"
                    )
                }
            )
        }
    }

    fun initiateRestore() {
        scope.launch {
            val file = showOpenFileChooser()
            if (file == null) return@launch
            uiState = uiState.copy(isLoading = true, error = null)
            val content = runCatching { file.readText() }.getOrElse {
                uiState = uiState.copy(isLoading = false, error = it.message ?: "Failed to read file")
                return@launch
            }
            val validation = restoreFromBackupUseCase.validateIntegrity(content)
            validation.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isLoading = false,
                        pendingRestoreContent = content,
                        showRestoreConfirmation = true,
                        pendingRestoreSummary = "Backup contains: ${it.projectCount} projects, ${it.taskCount} tasks, ${it.repositoryCount} repositories, ${it.ruleCount} rules. Exported: ${it.exportedAt}"
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Invalid backup file"
                    )
                }
            )
        }
    }

    fun confirmRestore() {
        val content = uiState.pendingRestoreContent ?: return
        uiState = uiState.copy(
            showRestoreConfirmation = false,
            pendingRestoreContent = null,
            pendingRestoreSummary = null,
            isLoading = true
        )
        scope.launch {
            val result = restoreFromBackupUseCase.apply(content)
            result.fold(
                onSuccess = { importResult ->
                    val summary = buildString {
                        append("Restore completed: ")
                        append("${importResult.projectsCreated} projects, ")
                        append("${importResult.tasksCreated} tasks, ")
                        append("${importResult.reposCreated} repositories, ")
                        append("${importResult.rulesCreated} rules")
                    }
                    uiState = uiState.copy(
                        isLoading = false,
                        restoreFeedback = summary
                    )
                },
                onFailure = { e ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = e.message ?: "Restore failed"
                    )
                }
            )
        }
    }

    fun cancelRestore() {
        uiState = uiState.copy(
            showRestoreConfirmation = false,
            pendingRestoreContent = null,
            pendingRestoreSummary = null
        )
    }

    fun clearRestoreFeedback() {
        uiState = uiState.copy(restoreFeedback = null)
    }

    fun clearMcpFeedback() {
        uiState = uiState.copy(mcpFeedback = null, mcpError = null)
    }
}

data class LlmConfigurationEditorState(
    val id: UUID? = null,
    val name: String = "",
    val endpointUrl: String = "",
    val modelIdentifier: String = "",
    val apiKey: String = "",
    val isDefault: Boolean = false
)

data class AgentDefinitionEditorState(
    val id: UUID? = null,
    val name: String = "",
    val description: String = "",
    val promptTemplate: String = "",
    val llmConfigurationId: UUID? = null,
    val scope: AgentScope = AgentScope.GLOBAL,
    val projectId: UUID? = null,
    val trigger: AgentTrigger = AgentTrigger.MANUAL,
    val isEnabled: Boolean = true
)

data class McpServerEditorState(
    val id: UUID? = null,
    val isEnabled: Boolean = false,
    val port: Int = 3333
)

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportFeedback: String? = null,
    val importFeedback: String? = null,
    val restoreFeedback: String? = null,
    val pendingRestoreContent: String? = null,
    val showRestoreConfirmation: Boolean = false,
    val pendingRestoreSummary: String? = null,
    val isLlmLoading: Boolean = false,
    val isLlmSaving: Boolean = false,
    val isLlmTesting: Boolean = false,
    val llmError: String? = null,
    val llmFeedback: String? = null,
    val llmConfigurations: List<LlmConfiguration> = emptyList(),
    val llmEditor: LlmConfigurationEditorState = LlmConfigurationEditorState(),
    val projects: List<com.aitask.core.domain.model.Project> = emptyList(),
    val isAgentLoading: Boolean = false,
    val isAgentSaving: Boolean = false,
    val agentError: String? = null,
    val agentFeedback: String? = null,
    val agentDefinitions: List<AgentDefinition> = emptyList(),
    val agentEditor: AgentDefinitionEditorState = AgentDefinitionEditorState(),
    val isMcpLoading: Boolean = false,
    val isMcpSaving: Boolean = false,
    val mcpError: String? = null,
    val mcpFeedback: String? = null,
    val mcpManifest: com.aitask.core.domain.service.McpServerManifest = com.aitask.core.domain.service.McpServerManifest(
        serverName = "AiTask MCP bridge",
        endpointUrl = "http://127.0.0.1:3333/mcp",
        capabilities = listOf("task-context", "project-context", "repository-context"),
        tools = emptyList(),
        resources = emptyList(),
        safetyNotes = listOf("MCP bridge disabled")
    ),
    val mcpEditor: McpServerEditorState = McpServerEditorState()
)
