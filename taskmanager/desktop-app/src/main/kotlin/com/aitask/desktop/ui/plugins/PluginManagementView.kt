package com.aitask.desktop.ui.plugins

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.plugin.PluginCatalogItem
import com.aitask.core.domain.plugin.PluginCatalogFixtures
import com.aitask.core.domain.plugin.PluginLifecycleState
import com.aitask.desktop.ui.viewmodel.PluginManagementViewModel

private val HEALTHY_COLOR = Color(0xFF2E7D32)
private val ACTIVE_COLOR = Color(0xFF1565C0)
private val WARNING_COLOR = Color(0xFFF57C00)
private val DISABLED_COLOR = Color(0xFF757575)
private val ERROR_COLOR = Color(0xFFC62828)

@Composable
fun PluginManagementView(
    viewModel: PluginManagementViewModel = remember { PluginManagementViewModel() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.loadCatalog()
    }
    val uiState = viewModel.uiState
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PluginManagementHeader()
        uiState.feedback?.let { feedback ->
            FeedbackBanner(message = feedback.message, isError = feedback.isError)
        }
        CoreFeaturesCard(features = uiState.coreFeatures)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        OptionalPluginsCard(
            plugins = uiState.plugins,
            actionInProgress = uiState.actionInProgress,
            onInstall = viewModel::install,
            onAttach = viewModel::attach,
            onEnable = viewModel::enable,
            onDisable = viewModel::disable,
            onDetach = viewModel::detach,
            onRemove = viewModel::remove
        )
    }
}

@Composable
private fun PluginManagementHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Plugin catalog",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "Manage optional add-ons",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Core AiTask features stay available at all times. Optional plugins can be installed, attached, enabled, disabled, detached, or removed without affecting the rest of the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun FeedbackBanner(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoreFeaturesCard(features: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Core platform features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These capabilities are built into AiTask and are always available, regardless of optional plugins.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = feature,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionalPluginsCard(
    plugins: List<PluginCatalogItem>,
    actionInProgress: String?,
    onInstall: (String) -> Unit,
    onAttach: (String) -> Unit,
    onEnable: (String) -> Unit,
    onDisable: (String) -> Unit,
    onDetach: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Optional plugins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Supported plugins are listed here with their current install, attach, and enablement state.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (plugins.isEmpty()) {
                Text("No plugins are available yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    plugins.forEach { plugin ->
                        PluginCard(
                            plugin = plugin,
                            actionInProgress = actionInProgress == plugin.id,
                            onInstall = onInstall,
                            onAttach = onAttach,
                            onEnable = onEnable,
                            onDisable = onDisable,
                            onDetach = onDetach,
                            onRemove = onRemove
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: PluginCatalogItem,
    actionInProgress: Boolean,
    onInstall: (String) -> Unit,
    onAttach: (String) -> Unit,
    onEnable: (String) -> Unit,
    onDisable: (String) -> Unit,
    onDetach: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val statusColor = when (plugin.lifecycleState) {
        PluginLifecycleState.ENABLED -> HEALTHY_COLOR
        PluginLifecycleState.INITIALIZED -> ACTIVE_COLOR
        PluginLifecycleState.DISCOVERED -> DISABLED_COLOR
        PluginLifecycleState.DISABLED -> WARNING_COLOR
        PluginLifecycleState.FAILED -> ERROR_COLOR
        PluginLifecycleState.BLOCKED -> ERROR_COLOR
        PluginLifecycleState.REMOVED -> DISABLED_COLOR
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = if (plugin.optional) "Optional" else "Core",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "Version ${plugin.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = plugin.statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                    Text(
                        text = plugin.statusDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PluginStatePill("Installed", plugin.installed, HEALTHY_COLOR)
                PluginStatePill("Attached", plugin.attached, ACTIVE_COLOR)
                PluginStatePill("Enabled", plugin.enabled, WARNING_COLOR)
            }
            val lastActionMessage = plugin.lastActionMessage
            if (lastActionMessage != null) {
                Text(
                    text = lastActionMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (!plugin.installed) {
                    Button(onClick = { onInstall(plugin.id) }, enabled = !actionInProgress) {
                        Text("Install")
                    }
                } else {
                    if (!plugin.attached) {
                        Button(onClick = { onAttach(plugin.id) }, enabled = !actionInProgress) {
                            Text("Attach")
                        }
                    }
                    if (plugin.attached && !plugin.enabled) {
                        Button(onClick = { onEnable(plugin.id) }, enabled = !actionInProgress) {
                            Text("Enable")
                        }
                    }
                    if (plugin.enabled) {
                        OutlinedButton(onClick = { onDisable(plugin.id) }, enabled = !actionInProgress) {
                            Text("Disable")
                        }
                    }
                    if (plugin.attached) {
                        OutlinedButton(onClick = { onDetach(plugin.id) }, enabled = !actionInProgress) {
                            Text("Detach")
                        }
                    }
                    Button(
                        onClick = { onRemove(plugin.id) },
                        enabled = !actionInProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginStatePill(label: String, active: Boolean, accent: Color) {
    Surface(
        color = if (active) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = "$label: ${if (active) "Yes" else "No"}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
