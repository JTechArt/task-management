package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.plugin.PluginCatalogFixtures
import com.aitask.core.domain.plugin.PluginCatalogItem
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
    val isLoading: Boolean = false,
    val actionInProgress: String? = null,
    val feedback: PluginManagementFeedback? = null
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
                uiState = uiState.copy(
                    plugins = pluginManagementService.catalog(),
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
                        uiState = uiState.copy(
                            plugins = pluginManagementService.catalog(),
                            actionInProgress = null,
                            feedback = PluginManagementFeedback(
                                message = "${item.name} ${actionPastTense(actionLabel)} successfully"
                            )
                        )
                    },
                    onFailure = { error ->
                        uiState = uiState.copy(
                            plugins = pluginManagementService.catalog(),
                            actionInProgress = null,
                            feedback = PluginManagementFeedback(
                                message = error.message ?: "Plugin $actionLabel failed",
                                isError = true
                            )
                        )
                    }
                )
            } catch (e: Exception) {
                uiState = uiState.copy(
                    plugins = pluginManagementService.catalog(),
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
