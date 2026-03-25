package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.LlmConfigurationRequest
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.service.LlmConnectionValidator
import com.aitask.core.domain.usecase.CreateBackupUseCase
import com.aitask.core.domain.usecase.ExportDataUseCase
import com.aitask.core.domain.usecase.ImportDataUseCase
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
    private val llmConfigurationRepository: LlmConfigurationRepository = DependencyContainer.llmConfigurationRepository,
    private val llmConnectionValidator: LlmConnectionValidator = DependencyContainer.llmConnectionValidator,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        loadLlmConfigurations()
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

    fun startNewLlmConfiguration() {
        uiState = uiState.copy(
            llmEditor = LlmConfigurationEditorState(),
            llmError = null,
            llmFeedback = null
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

    fun updateLlmEditorName(name: String) = updateLlmEditor { it.copy(name = name) }

    fun updateLlmEditorEndpoint(endpointUrl: String) = updateLlmEditor { it.copy(endpointUrl = endpointUrl) }

    fun updateLlmEditorModel(modelIdentifier: String) = updateLlmEditor { it.copy(modelIdentifier = modelIdentifier) }

    fun updateLlmEditorApiKey(apiKey: String) = updateLlmEditor { it.copy(apiKey = apiKey) }

    fun updateLlmEditorDefault(isDefault: Boolean) = updateLlmEditor { it.copy(isDefault = isDefault) }

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

    private fun updateLlmEditor(transform: (LlmConfigurationEditorState) -> LlmConfigurationEditorState) {
        uiState = uiState.copy(
            llmEditor = transform(uiState.llmEditor),
            llmError = null,
            llmFeedback = null
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
}

data class LlmConfigurationEditorState(
    val id: UUID? = null,
    val name: String = "",
    val endpointUrl: String = "",
    val modelIdentifier: String = "",
    val apiKey: String = "",
    val isDefault: Boolean = false
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
    val llmEditor: LlmConfigurationEditorState = LlmConfigurationEditorState()
)
