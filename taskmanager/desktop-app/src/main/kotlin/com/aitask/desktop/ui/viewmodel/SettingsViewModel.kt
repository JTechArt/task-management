package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.usecase.ExportDataUseCase
import com.aitask.core.domain.usecase.ImportDataUseCase
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class SettingsViewModel(
    private val exportDataUseCase: ExportDataUseCase = DependencyContainer.exportDataUseCase,
    private val importDataUseCase: ImportDataUseCase = DependencyContainer.importDataUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    var uiState by mutableStateOf(SettingsUiState())
        private set

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

    private suspend fun showSaveFileChooser(): File? = withContext(Dispatchers.Main) {
        JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("JSON files (*.json)", "json")
            dialogTitle = "Export Data"
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
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportFeedback: String? = null,
    val importFeedback: String? = null
)
