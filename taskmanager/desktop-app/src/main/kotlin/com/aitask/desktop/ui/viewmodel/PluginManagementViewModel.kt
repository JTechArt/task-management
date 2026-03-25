package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.plugin.PluginCatalogFixtures
import com.aitask.core.domain.plugin.PluginCatalogItem
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.plugin.PluginConfigurationValidationResult
import com.aitask.core.domain.plugin.PluginHealthReport
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PluginManagementFeedback(
    val message: String,
    val isError: Boolean = false
)

data class PluginManagementUiState(
    val coreFeatures: List<String> = PluginCatalogFixtures.coreFeatureHighlights,
    val plugins: List<PluginCatalogItem> = emptyList(),
    val validationByPluginId: Map<String, PluginConfigurationValidationResult> = emptyMap(),
    val healthByPluginId: Map<String, PluginHealthReport> = emptyMap(),
    val recentActivityByPluginId: Map<String, List<Activity>> = emptyMap(),
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,
    val operationalRefreshInProgress: String? = null,
    val configurationInProgress: Boolean = false,
    val feedback: PluginManagementFeedback? = null,
    val configurationEditor: PluginConfigurationEditorState? = null
)

data class PluginConfigurationEditorState(
    val plugin: PluginCatalogItem,
    val scopeKey: String = "",
    val fields: Map<String, String> = emptyMap(),
    val secrets: Map<String, String> = emptyMap(),
    val schedule: String = "",
    val validationResult: PluginConfigurationValidationResult? = null
)

class PluginManagementViewModel(
    private val pluginManagementService: PluginManagementService = DependencyContainer.pluginManagementService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    var uiState by mutableStateOf(PluginManagementUiState())
        private set

    fun loadCatalog() {
        scope.launch {
            uiState = uiState.copy(isLoading = true, feedback = null)
            try {
                val plugins = pluginManagementService.catalog()
                val operationalState = loadOperationalState(plugins)
                uiState = uiState.copy(
                    plugins = plugins,
                    validationByPluginId = operationalState.validationByPluginId,
                    healthByPluginId = operationalState.healthByPluginId,
                    recentActivityByPluginId = operationalState.recentActivityByPluginId,
                    isLoading = false
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    feedback = PluginManagementFeedback(
                        message = e.message ?: "Failed to load plugin catalog",
                        isError = true
                    )
                )
            }
        }
    }

    fun install(pluginId: String) = performAction(pluginId, "install") { pluginManagementService.install(pluginId) }

    fun attach(pluginId: String) = performAction(pluginId, "attach") { pluginManagementService.attach(pluginId) }

    fun enable(pluginId: String) = performAction(pluginId, "enable") { pluginManagementService.enable(pluginId) }

    fun disable(pluginId: String) = performAction(pluginId, "disable") { pluginManagementService.disable(pluginId) }

    fun detach(pluginId: String) = performAction(pluginId, "detach") { pluginManagementService.detach(pluginId) }

    fun remove(pluginId: String) = performAction(pluginId, "remove") { pluginManagementService.remove(pluginId) }

    fun refreshOperationalData(pluginId: String) {
        scope.launch {
            uiState = uiState.copy(operationalRefreshInProgress = pluginId, feedback = null)
            val plugin = uiState.plugins.firstOrNull { it.id == pluginId }
                ?: run {
                    uiState = uiState.copy(
                        operationalRefreshInProgress = null,
                        feedback = PluginManagementFeedback(
                            message = "Plugin '$pluginId' is not loaded",
                            isError = true
                        )
                    )
                    return@launch
                }
            val validationResult = pluginManagementService.validateConfiguration(
                pluginId = plugin.id,
                scope = plugin.configurationScope,
                scopeKey = pluginManagementService.getConfiguration(plugin.id, plugin.configurationScope, null)?.scopeKey
            ).getOrNull()
            val healthResult = pluginManagementService.health(plugin.id).getOrNull()
            val recentActivity = pluginManagementService.recentActivity(plugin.id)
            val updatedValidation = validationResult ?: uiState.validationByPluginId[plugin.id]
            val updatedHealth = healthResult ?: uiState.healthByPluginId[plugin.id]
            uiState = uiState.copy(
                validationByPluginId = if (updatedValidation != null) {
                    uiState.validationByPluginId + (plugin.id to updatedValidation)
                } else {
                    uiState.validationByPluginId
                },
                healthByPluginId = if (updatedHealth != null) {
                    uiState.healthByPluginId + (plugin.id to updatedHealth)
                } else {
                    uiState.healthByPluginId
                },
                recentActivityByPluginId = uiState.recentActivityByPluginId + (plugin.id to recentActivity),
                operationalRefreshInProgress = null,
                feedback = PluginManagementFeedback(
                    message = "${plugin.name} operational data refreshed"
                )
            )
        }
    }

    fun openConfigurationEditor(pluginId: String) {
        scope.launch {
            val plugin = uiState.plugins.firstOrNull { it.id == pluginId }
                ?: run {
                    uiState = uiState.copy(
                        feedback = PluginManagementFeedback(
                            message = "Plugin '$pluginId' is not loaded",
                            isError = true
                        )
                    )
                    return@launch
                }
            val snapshot = pluginManagementService.getConfiguration(
                pluginId = pluginId,
                scope = plugin.configurationScope,
                scopeKey = null
            )
            val validationResult = pluginManagementService.validateConfiguration(
                pluginId = pluginId,
                scope = plugin.configurationScope,
                scopeKey = snapshot?.scopeKey
            ).getOrNull()
            uiState = uiState.copy(
                configurationEditor = PluginConfigurationEditorState(
                    plugin = plugin,
                    scopeKey = snapshot?.scopeKey.orEmpty(),
                    fields = snapshot?.fields ?: emptyMap(),
                    secrets = snapshot?.secrets ?: emptyMap(),
                    schedule = snapshot?.schedule.orEmpty(),
                    validationResult = validationResult
                ),
                feedback = null
            )
        }
    }

    fun closeConfigurationEditor() {
        uiState = uiState.copy(configurationEditor = null, configurationInProgress = false)
    }

    fun updateConfigurationField(fieldId: String, value: String) {
        updateConfigurationEditor { editor ->
            editor.copy(fields = editor.fields + (fieldId to value))
        }
    }

    fun updateConfigurationSecret(fieldId: String, value: String) {
        updateConfigurationEditor { editor ->
            editor.copy(secrets = editor.secrets + (fieldId to value))
        }
    }

    fun updateConfigurationSchedule(value: String) {
        updateConfigurationEditor { editor ->
            editor.copy(schedule = value)
        }
    }

    fun updateConfigurationScopeKey(value: String) {
        updateConfigurationEditor { editor ->
            editor.copy(scopeKey = value)
        }
    }

    fun validateConfiguration() {
        val editor = uiState.configurationEditor ?: return
        scope.launch {
            uiState = uiState.copy(configurationInProgress = true, feedback = null)
            val validationResult = pluginManagementService.validateConfiguration(
                editor.toSnapshot()
            ).getOrNull()
            val recentActivity = pluginManagementService.recentActivity(editor.plugin.id)
            uiState = uiState.copy(
                configurationEditor = editor.copy(validationResult = validationResult),
                validationByPluginId = validationResult?.let { uiState.validationByPluginId + (editor.plugin.id to it) } ?: uiState.validationByPluginId,
                recentActivityByPluginId = uiState.recentActivityByPluginId + (editor.plugin.id to recentActivity),
                configurationInProgress = false,
                feedback = PluginManagementFeedback(
                    message = validationResult?.message ?: "Configuration validated"
                )
            )
        }
    }

    fun saveConfiguration() {
        val editor = uiState.configurationEditor ?: return
        scope.launch {
            uiState = uiState.copy(configurationInProgress = true, feedback = null)
            val snapshot = editor.toSnapshot()
            val result = pluginManagementService.saveConfiguration(snapshot)
            result.fold(
                onSuccess = { saved ->
                    val validationResult = pluginManagementService.validateConfiguration(
                        saved
                    ).getOrNull()
                    val recentActivity = pluginManagementService.recentActivity(saved.pluginId)
                    uiState = uiState.copy(
                        plugins = pluginManagementService.catalog(),
                        configurationEditor = editor.copy(
                            scopeKey = saved.scopeKey.orEmpty(),
                            fields = saved.fields,
                            secrets = saved.secrets,
                            schedule = saved.schedule.orEmpty(),
                            validationResult = validationResult
                        ),
                        validationByPluginId = validationResult?.let { uiState.validationByPluginId + (saved.pluginId to it) } ?: uiState.validationByPluginId,
                        recentActivityByPluginId = uiState.recentActivityByPluginId + (saved.pluginId to recentActivity),
                        configurationInProgress = false,
                        feedback = PluginManagementFeedback(
                            message = "${editor.plugin.name} configuration saved"
                        )
                    )
                },
                onFailure = { error ->
                    val validationResult = (error as? com.aitask.core.domain.plugin.PluginConfigurationValidationException)?.validationResult
                    val recentActivity = pluginManagementService.recentActivity(editor.plugin.id)
                    uiState = uiState.copy(
                        configurationEditor = editor.copy(validationResult = validationResult),
                        validationByPluginId = validationResult?.let { uiState.validationByPluginId + (editor.plugin.id to it) } ?: uiState.validationByPluginId,
                        recentActivityByPluginId = uiState.recentActivityByPluginId + (editor.plugin.id to recentActivity),
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

    private fun updateConfigurationEditor(transform: (PluginConfigurationEditorState) -> PluginConfigurationEditorState) {
        val editor = uiState.configurationEditor ?: return
        uiState = uiState.copy(configurationEditor = transform(editor))
    }

    private suspend fun loadOperationalState(
        plugins: List<PluginCatalogItem>
    ): OperationalState {
        val validationByPluginId = mutableMapOf<String, PluginConfigurationValidationResult>()
        val healthByPluginId = mutableMapOf<String, PluginHealthReport>()
        val recentActivityByPluginId = mutableMapOf<String, List<Activity>>()

        plugins.forEach { plugin ->
            pluginManagementService.validateConfiguration(
                pluginId = plugin.id,
                scope = plugin.configurationScope,
                scopeKey = pluginManagementService.getConfiguration(plugin.id, plugin.configurationScope, null)?.scopeKey
            ).getOrNull()?.let { validationByPluginId[plugin.id] = it }
            pluginManagementService.health(plugin.id).getOrNull()?.let { healthByPluginId[plugin.id] = it }
            recentActivityByPluginId[plugin.id] = pluginManagementService.recentActivity(plugin.id)
        }

        return OperationalState(
            validationByPluginId = validationByPluginId,
            healthByPluginId = healthByPluginId,
            recentActivityByPluginId = recentActivityByPluginId
        )
    }

    private data class OperationalState(
        val validationByPluginId: Map<String, PluginConfigurationValidationResult>,
        val healthByPluginId: Map<String, PluginHealthReport>,
        val recentActivityByPluginId: Map<String, List<Activity>>
    )

    private fun PluginConfigurationEditorState.toSnapshot(): PluginConfigurationSnapshot =
        PluginConfigurationSnapshot(
            pluginId = plugin.id,
            scope = plugin.configurationScope,
            scopeKey = scopeKey.takeIf { it.isNotBlank() },
            fields = fields,
            secrets = secrets,
            schedule = schedule.takeIf { it.isNotBlank() }
        )

    private fun performAction(
        pluginId: String,
        actionLabel: String,
        action: suspend () -> Result<PluginCatalogItem>
    ) {
        scope.launch {
            uiState = uiState.copy(actionInProgress = pluginId, feedback = null)
            try {
                action().fold(
                    onSuccess = { item ->
                        val plugins = pluginManagementService.catalog()
                        val operationalState = loadOperationalState(plugins)
                        uiState = uiState.copy(
                            plugins = plugins,
                            validationByPluginId = operationalState.validationByPluginId,
                            healthByPluginId = operationalState.healthByPluginId,
                            recentActivityByPluginId = operationalState.recentActivityByPluginId,
                            actionInProgress = null,
                            feedback = PluginManagementFeedback(
                                message = "${item.name} ${actionPastTense(actionLabel)} successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        val plugins = pluginManagementService.catalog()
                        val operationalState = loadOperationalState(plugins)
                        uiState = uiState.copy(
                            plugins = plugins,
                            validationByPluginId = operationalState.validationByPluginId,
                            healthByPluginId = operationalState.healthByPluginId,
                            recentActivityByPluginId = operationalState.recentActivityByPluginId,
                            actionInProgress = null,
                            feedback = PluginManagementFeedback(
                                message = error.message ?: "Plugin $actionLabel failed",
                                isError = true
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                val plugins = pluginManagementService.catalog()
                val operationalState = loadOperationalState(plugins)
                uiState = uiState.copy(
                    plugins = plugins,
                    validationByPluginId = operationalState.validationByPluginId,
                    healthByPluginId = operationalState.healthByPluginId,
                    recentActivityByPluginId = operationalState.recentActivityByPluginId,
                    actionInProgress = null,
                    feedback = PluginManagementFeedback(
                        message = e.message ?: "Plugin $actionLabel failed",
                        isError = true
                    )
                )
            }
        }
    }

    private fun actionPastTense(actionLabel: String): String = when (actionLabel) {
        "install" -> "installed"
        "attach" -> "attached"
        "enable" -> "enabled"
        "disable" -> "disabled"
        "detach" -> "detached"
        "remove" -> "removed"
        else -> "$actionLabel completed"
    }
}
