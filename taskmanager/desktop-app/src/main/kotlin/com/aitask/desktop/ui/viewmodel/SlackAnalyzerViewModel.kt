package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.PluginConfigurationValidationException
import com.aitask.core.domain.plugin.mergePluginConfigurationFields
import com.aitask.core.domain.plugin.splitPluginConfigurationFields
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.SlackAnalysisChannelProgress
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.core.domain.service.SlackChannelAnalysisService
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SlackRunHistoryFilters(
    val dateFrom: String = "",
    val dateTo: String = "",
    val status: ActivityStatus? = null,
    val trigger: SlackAnalysisTrigger? = null
)

data class SlackAnalyzerUiState(
    val isLoading: Boolean = false,
    val editor: PluginConfigurationEditorState? = null,
    val configurationInProgress: Boolean = false,
    val feedback: PluginManagementFeedback? = null,
    val error: String? = null,
    val analysisRunInProgress: Boolean = false,
    val analysisProgressMessage: String? = null,
    val analysisLastSummary: String? = null,
    val analysisLastError: String? = null,
    val analysisChannelProgress: SlackAnalysisChannelProgress? = null,
    val runHistory: List<Activity> = emptyList(),
    val runHistoryLoading: Boolean = false,
    val runHistoryError: String? = null,
    val runHistoryFilters: SlackRunHistoryFilters = SlackRunHistoryFilters()
)

class SlackAnalyzerViewModel(
    private val pluginManagementService: PluginManagementService = DependencyContainer.pluginManagementService,
    private val slackChannelAnalysisService: SlackChannelAnalysisService = DependencyContainer.slackChannelAnalysisService,
    private val activityRepository: ActivityRepository = DependencyContainer.activityRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    companion object {
        const val PLUGIN_ID: String = "plugin.slack-channel-analyzer"
        private const val RUN_HISTORY_LIMIT = 200
    }

    var uiState by mutableStateOf(SlackAnalyzerUiState())
        private set

    fun load() {
        scope.launch {
            uiState = uiState.copy(isLoading = true, feedback = null, error = null)
            loadRunHistory()
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

    fun loadRunHistory() {
        scope.launch {
            uiState = uiState.copy(runHistoryLoading = true, runHistoryError = null)
            try {
                val filters = uiState.runHistoryFilters
                val zoneId: ZoneId = ZoneId.systemDefault()
                val afterInclusive: Instant? = parseDate(filters.dateFrom)?.atStartOfDay(zoneId)?.toInstant()
                val beforeExclusive: Instant? = parseDate(filters.dateTo)?.plusDays(1)?.atStartOfDay(zoneId)?.toInstant()
                val loaded: List<Activity> = activityRepository.findFiltered(
                    type = ActivityType.SLACK_ANALYSIS_RUN,
                    status = filters.status,
                    createdAfterInclusive = afterInclusive,
                    createdBeforeExclusive = beforeExclusive,
                    limit = RUN_HISTORY_LIMIT
                )
                val triggerFiltered: List<Activity> = if (filters.trigger == null) {
                    loaded
                } else {
                    loaded.filter { act -> act.metadata["trigger"] == filters.trigger.name }
                }
                uiState = uiState.copy(runHistory = triggerFiltered, runHistoryLoading = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    runHistoryLoading = false,
                    runHistoryError = e.message ?: "Failed to load Slack analysis run history"
                )
            }
        }
    }

    fun setRunHistoryDateFrom(raw: String) {
        uiState = uiState.copy(runHistoryFilters = uiState.runHistoryFilters.copy(dateFrom = raw))
        loadRunHistory()
    }

    fun setRunHistoryDateTo(raw: String) {
        uiState = uiState.copy(runHistoryFilters = uiState.runHistoryFilters.copy(dateTo = raw))
        loadRunHistory()
    }

    fun setRunHistoryStatusFilter(status: ActivityStatus?) {
        uiState = uiState.copy(runHistoryFilters = uiState.runHistoryFilters.copy(status = status))
        loadRunHistory()
    }

    fun setRunHistoryTriggerFilter(trigger: SlackAnalysisTrigger?) {
        uiState = uiState.copy(runHistoryFilters = uiState.runHistoryFilters.copy(trigger = trigger))
        loadRunHistory()
    }

    fun clearRunHistoryFilters() {
        uiState = uiState.copy(runHistoryFilters = SlackRunHistoryFilters())
        loadRunHistory()
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
                analysisLastSummary = null,
                analysisChannelProgress = null
            )
            val result = slackChannelAnalysisService.runIncrementalAnalysis(
                trigger = SlackAnalysisTrigger.MANUAL,
                onProgress = { message: String ->
                    uiState = uiState.copy(analysisProgressMessage = message)
                },
                onChannelProgress = { progress: SlackAnalysisChannelProgress ->
                    uiState = uiState.copy(analysisChannelProgress = progress)
                }
            )
            uiState = uiState.copy(analysisChannelProgress = null)
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
            loadRunHistory()
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

private fun parseDate(raw: String): LocalDate? {
    val s: String = raw.trim()
    if (s.isEmpty()) {
        return null
    }
    return runCatching { LocalDate.parse(s) }.getOrNull()
}
