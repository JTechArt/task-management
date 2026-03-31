package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.PluginConfigurationValidationException
import com.aitask.core.domain.plugin.mergePluginConfigurationFields
import com.aitask.core.domain.plugin.splitPluginConfigurationFields
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.core.domain.service.SlackChannelAnalysisService
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SlackAnalyzerUiState(
    val isLoading: Boolean = false,
    val editor: PluginConfigurationEditorState? = null,
    val configurationInProgress: Boolean = false,
    val feedback: PluginManagementFeedback? = null,
    val error: String? = null,
    val analysisRunInProgress: Boolean = false,
    val analysisProgressMessage: String? = null,
    val analysisLastSummary: String? = null,
    val analysisLastError: String? = null
)

class SlackAnalyzerViewModel(
    private val pluginManagementService: PluginManagementService = DependencyContainer.pluginManagementService,
    private val slackChannelAnalysisService: SlackChannelAnalysisService = DependencyContainer.slackChannelAnalysisService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        const val PLUGIN_ID: String = "plugin.slack-channel-analyzer"
    }

    var uiState by mutableStateOf(SlackAnalyzerUiState())
        private set

    fun load() {
        scope.launch {
            uiState = uiState.copy(isLoading = true, feedback = null, error = null)
            try {
                val plugins = pluginManagementService.catalog()
                val plugin = plugins.firstOrNull { it.id == PLUGIN_ID }
                if (plugin == null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "Slack Channel Analyzer plugin is not available."
                    )
                    return@launch
                }
                val snapshot = pluginManagementService.getConfiguration(
                    pluginId = PLUGIN_ID,
                    scope = plugin.configurationScope,
                    scopeKey = null
                )
                val validationResult = pluginManagementService.validateConfiguration(
                    pluginId = PLUGIN_ID,
                    scope = plugin.configurationScope,
                    scopeKey = snapshot?.scopeKey
                ).getOrNull()
                val split = splitPluginConfigurationFields(
                    plugin = plugin,
                    snapshotFields = snapshot?.fields ?: emptyMap()
                )
                uiState = uiState.copy(
                    isLoading = false,
                    editor = PluginConfigurationEditorState(
                        plugin = plugin,
                        scopeKey = snapshot?.scopeKey.orEmpty(),
                        fields = split.visible,
                        internalFields = split.internal,
                        secrets = snapshot?.secrets ?: emptyMap(),
                        schedule = snapshot?.schedule.orEmpty(),
                        validationResult = validationResult
                    )
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load Slack Analyzer configuration"
                )
            }
        }
    }

    fun updateConfigurationField(fieldId: String, value: String) {
        updateEditor { it.copy(fields = it.fields + (fieldId to value)) }
    }

    fun updateConfigurationSecret(fieldId: String, value: String) {
        updateEditor { it.copy(secrets = it.secrets + (fieldId to value)) }
    }

    fun updateConfigurationSchedule(value: String) {
        updateEditor { it.copy(schedule = value) }
    }

    fun validateConfiguration() {
        val editor = uiState.editor ?: return
        scope.launch {
            uiState = uiState.copy(configurationInProgress = true, feedback = null)
            val validationResult = pluginManagementService.validateConfiguration(editor.toSnapshot()).getOrNull()
            uiState = uiState.copy(
                editor = editor.copy(validationResult = validationResult),
                configurationInProgress = false,
                feedback = PluginManagementFeedback(
                    message = validationResult?.message ?: "Configuration validated"
                )
            )
        }
    }

    fun saveConfiguration() {
        val editor = uiState.editor ?: return
        scope.launch {
            uiState = uiState.copy(configurationInProgress = true, feedback = null)
            val snapshot = editor.toSnapshot()
            val result = pluginManagementService.saveConfiguration(snapshot)
            result.fold(
                onSuccess = { saved ->
                    val validationResult = pluginManagementService.validateConfiguration(saved).getOrNull()
                    val splitSaved = splitPluginConfigurationFields(
                        plugin = editor.plugin,
                        snapshotFields = saved.fields
                    )
                    uiState = uiState.copy(
                        editor = editor.copy(
                            scopeKey = saved.scopeKey.orEmpty(),
                            fields = splitSaved.visible,
                            internalFields = splitSaved.internal,
                            secrets = saved.secrets,
                            schedule = saved.schedule.orEmpty(),
                            validationResult = validationResult
                        ),
                        configurationInProgress = false,
                        feedback = PluginManagementFeedback(message = "Slack Analyzer configuration saved")
                    )
                },
                onFailure = { error ->
                    val validationResult = (error as? PluginConfigurationValidationException)?.validationResult
                    uiState = uiState.copy(
                        editor = editor.copy(validationResult = validationResult),
                        configurationInProgress = false,
                        feedback = PluginManagementFeedback(
                            message = error.message ?: "Failed to save configuration",
                            isError = true
                        )
                    )
                }
            )
        }
    }

    fun runAnalysis() {
        scope.launch {
            uiState = uiState.copy(
                analysisRunInProgress = true,
                analysisProgressMessage = "Starting…",
                analysisLastError = null,
                analysisLastSummary = null
            )
            val result = slackChannelAnalysisService.runIncrementalAnalysis(
                trigger = SlackAnalysisTrigger.MANUAL,
                onProgress = { message: String ->
                    uiState = uiState.copy(analysisProgressMessage = message)
                }
            )
            result.fold(
                onSuccess = { runResult ->
                    uiState = uiState.copy(
                        analysisRunInProgress = false,
                        analysisProgressMessage = null,
                        analysisLastSummary = runResult.summaryMessage
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        analysisRunInProgress = false,
                        analysisProgressMessage = null,
                        analysisLastError = error.message ?: "Failed to run Slack analysis"
                    )
                }
            )
            refreshEditorAfterAnalysisRun()
        }
    }

    private fun refreshEditorAfterAnalysisRun() {
        scope.launch {
            try {
                val plugins = pluginManagementService.catalog()
                val plugin = plugins.firstOrNull { it.id == PLUGIN_ID } ?: return@launch
                val snapshot = pluginManagementService.getConfiguration(
                    pluginId = PLUGIN_ID,
                    scope = plugin.configurationScope,
                    scopeKey = null
                )
                val validationResult = pluginManagementService.validateConfiguration(
                    pluginId = PLUGIN_ID,
                    scope = plugin.configurationScope,
                    scopeKey = snapshot?.scopeKey
                ).getOrNull()
                val split = splitPluginConfigurationFields(
                    plugin = plugin,
                    snapshotFields = snapshot?.fields ?: emptyMap()
                )
                uiState = uiState.copy(
                    editor = PluginConfigurationEditorState(
                        plugin = plugin,
                        scopeKey = snapshot?.scopeKey.orEmpty(),
                        fields = split.visible,
                        internalFields = split.internal,
                        secrets = snapshot?.secrets ?: emptyMap(),
                        schedule = snapshot?.schedule.orEmpty(),
                        validationResult = validationResult
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun updateEditor(transform: (PluginConfigurationEditorState) -> PluginConfigurationEditorState) {
        val editor = uiState.editor ?: return
        uiState = uiState.copy(editor = transform(editor))
    }

    private fun PluginConfigurationEditorState.toSnapshot(): PluginConfigurationSnapshot =
        PluginConfigurationSnapshot(
            pluginId = plugin.id,
            scope = plugin.configurationScope,
            scopeKey = scopeKey.takeIf { it.isNotBlank() },
            fields = mergePluginConfigurationFields(visible = fields, internal = internalFields),
            secrets = secrets,
            schedule = schedule.takeIf { it.isNotBlank() }
        )
}
