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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.plugin.PluginCatalogItem
import com.aitask.core.domain.plugin.PluginCatalogFixtures
import com.aitask.core.domain.plugin.PluginConfigurationFieldType
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationValidationResult
import com.aitask.core.domain.plugin.PluginHealthReport
import com.aitask.core.domain.plugin.PluginPrerequisiteResult
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.plugin.PluginLifecycleState
import com.aitask.desktop.ui.viewmodel.PluginConfigurationEditorState
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
            validationByPluginId = uiState.validationByPluginId,
            healthByPluginId = uiState.healthByPluginId,
            recentActivityByPluginId = uiState.recentActivityByPluginId,
            operationalRefreshInProgress = uiState.operationalRefreshInProgress,
            onInstall = viewModel::install,
            onAttach = viewModel::attach,
            onEnable = viewModel::enable,
            onDisable = viewModel::disable,
            onDetach = viewModel::detach,
            onRemove = viewModel::remove,
            onConfigure = viewModel::openConfigurationEditor,
            onRefreshOperationalData = viewModel::refreshOperationalData
        )
        uiState.configurationEditor?.let { editor ->
            PluginConfigurationDialog(
                editor = editor,
                inProgress = uiState.configurationInProgress,
                onDismiss = viewModel::closeConfigurationEditor,
                onValidate = viewModel::validateConfiguration,
                onSave = viewModel::saveConfiguration,
                onScopeKeyChange = viewModel::updateConfigurationScopeKey,
                onFieldChange = viewModel::updateConfigurationField,
                onSecretChange = viewModel::updateConfigurationSecret,
                onScheduleChange = viewModel::updateConfigurationSchedule
            )
        }
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
    validationByPluginId: Map<String, PluginConfigurationValidationResult>,
    healthByPluginId: Map<String, PluginHealthReport>,
    recentActivityByPluginId: Map<String, List<Activity>>,
    operationalRefreshInProgress: String?,
    onInstall: (String) -> Unit,
    onAttach: (String) -> Unit,
    onEnable: (String) -> Unit,
    onDisable: (String) -> Unit,
    onDetach: (String) -> Unit,
    onRemove: (String) -> Unit,
    onConfigure: (String) -> Unit,
    onRefreshOperationalData: (String) -> Unit
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
                            validationResult = validationByPluginId[plugin.id],
                            healthReport = healthByPluginId[plugin.id],
                            recentActivity = recentActivityByPluginId[plugin.id].orEmpty(),
                            operationalRefreshInProgress = operationalRefreshInProgress == plugin.id,
                            onInstall = onInstall,
                            onAttach = onAttach,
                            onEnable = onEnable,
                            onDisable = onDisable,
                            onDetach = onDetach,
                            onRemove = onRemove,
                            onConfigure = onConfigure,
                            onRefreshOperationalData = onRefreshOperationalData
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
    validationResult: PluginConfigurationValidationResult?,
    healthReport: PluginHealthReport?,
    recentActivity: List<Activity>,
    operationalRefreshInProgress: Boolean,
    onInstall: (String) -> Unit,
    onAttach: (String) -> Unit,
    onEnable: (String) -> Unit,
    onDisable: (String) -> Unit,
    onDetach: (String) -> Unit,
    onRemove: (String) -> Unit,
    onConfigure: (String) -> Unit,
    onRefreshOperationalData: (String) -> Unit
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
            OperationalSummaryCard(
                plugin = plugin,
                validationResult = validationResult,
                healthReport = healthReport,
                recentActivity = recentActivity,
                operationalRefreshInProgress = operationalRefreshInProgress,
                onRefreshOperationalData = onRefreshOperationalData,
                modifier = Modifier.fillMaxWidth()
            )
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
                    if (plugin.installed && (plugin.configurationSchema.fields.isNotEmpty() || plugin.configurationSchema.prerequisites.isNotEmpty())) {
                        OutlinedButton(onClick = { onConfigure(plugin.id) }, enabled = !actionInProgress) {
                            Text("Configure")
                        }
                    }
                }
                OutlinedButton(
                    onClick = { onRefreshOperationalData(plugin.id) },
                    enabled = !actionInProgress && !operationalRefreshInProgress
                ) {
                    Text(if (operationalRefreshInProgress) "Refreshing..." else "Re-check")
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

@Composable
private fun OperationalSummaryCard(
    plugin: PluginCatalogItem,
    validationResult: PluginConfigurationValidationResult?,
    healthReport: PluginHealthReport?,
    recentActivity: List<Activity>,
    operationalRefreshInProgress: Boolean,
    onRefreshOperationalData: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val summaryStatus = operationalStatusLabel(plugin, validationResult, healthReport)
    val summaryColor = operationalStatusColor(plugin, validationResult, healthReport)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = summaryColor.copy(alpha = 0.14f), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        text = summaryStatus,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = summaryColor
                    )
                }
            }
            Text(
                text = operationalStatusMessage(validationResult, healthReport, plugin),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            if (validationResult?.fieldErrors?.isNotEmpty() == true) {
                Text(
                    text = "Configuration issues: ${validationResult.fieldErrors.values.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (healthReport?.detail != null || !healthReport?.diagnostics.isNullOrEmpty()) {
                Text(
                    text = healthReport?.detail ?: healthReport?.diagnostics?.firstOrNull()?.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            if (recentActivity.isNotEmpty()) {
                Divider()
                Text(
                    text = "Recent plugin events",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                recentActivity.take(3).forEach { activity ->
                    Text(
                        text = "• ${activity.description} (${activity.status.name.lowercase()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (activity.status == com.aitask.core.domain.model.ActivityStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

private fun operationalStatusLabel(
    plugin: PluginCatalogItem,
    validationResult: PluginConfigurationValidationResult?,
    healthReport: PluginHealthReport?
): String = when {
    !plugin.installed -> "Unavailable"
    validationResult?.valid == false -> "Misconfigured"
    healthReport?.state == HealthState.FAILED -> "Degraded"
    healthReport?.state == HealthState.DEGRADED -> "Degraded"
    plugin.enabled && healthReport?.state == HealthState.HEALTHY -> "Healthy"
    plugin.enabled -> "Enabled"
    plugin.attached -> "Disabled"
    else -> "Disabled"
}

private fun operationalStatusColor(
    plugin: PluginCatalogItem,
    validationResult: PluginConfigurationValidationResult?,
    healthReport: PluginHealthReport?
): Color = when (operationalStatusLabel(plugin, validationResult, healthReport)) {
    "Healthy" -> HEALTHY_COLOR
    "Enabled" -> ACTIVE_COLOR
    "Disabled" -> DISABLED_COLOR
    "Misconfigured" -> ERROR_COLOR
    "Degraded" -> WARNING_COLOR
    "Unavailable" -> ERROR_COLOR
    else -> ACTIVE_COLOR
}

private fun operationalStatusMessage(
    validationResult: PluginConfigurationValidationResult?,
    healthReport: PluginHealthReport?,
    plugin: PluginCatalogItem
): String = when {
    validationResult?.valid == false -> validationResult.message ?: "${plugin.name} needs configuration fixes"
    healthReport?.state == HealthState.FAILED -> healthReport.message
    healthReport?.state == HealthState.DEGRADED -> healthReport.message
    healthReport?.message != null -> healthReport.message
    plugin.enabled -> "${plugin.name} is enabled and ready"
    plugin.attached -> "${plugin.name} is attached and waiting to be enabled"
    else -> "${plugin.name} is installed but disabled"
}

@Composable
private fun PluginConfigurationDialog(
    editor: PluginConfigurationEditorState,
    inProgress: Boolean,
    onDismiss: () -> Unit,
    onValidate: () -> Unit,
    onSave: () -> Unit,
    onScopeKeyChange: (String) -> Unit,
    onFieldChange: (String, String) -> Unit,
    onSecretChange: (String, String) -> Unit,
    onScheduleChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure ${editor.plugin.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Scope: ${editor.plugin.configurationScope.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (editor.plugin.configurationScope == PluginConfigurationScope.PROJECT) {
                    OutlinedTextField(
                        value = editor.scopeKey,
                        onValueChange = onScopeKeyChange,
                        label = { Text("Project scope key") },
                        placeholder = { Text("Project identifier") },
                        supportingText = { Text("Use a stable project key so configuration stays isolated.") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !inProgress
                    )
                }
                if (editor.plugin.configurationSchema.fields.isEmpty() && editor.plugin.configurationSchema.prerequisites.isEmpty()) {
                    Text(
                        text = "This plugin does not require configuration.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    editor.plugin.configurationSchema.fields.forEach { field ->
                        ConfigurationFieldEditor(
                            field = field,
                            value = when (field.type) {
                                PluginConfigurationFieldType.SECRET -> editor.secrets[field.id].orEmpty()
                                PluginConfigurationFieldType.SCHEDULE -> editor.schedule
                                else -> editor.fields[field.id].orEmpty()
                            },
                            error = editor.validationResult?.fieldErrors?.get(field.id),
                            enabled = !inProgress,
                            onValueChange = { value ->
                                when (field.type) {
                                    PluginConfigurationFieldType.SECRET -> onSecretChange(field.id, value)
                                    PluginConfigurationFieldType.SCHEDULE -> onScheduleChange(value)
                                    else -> onFieldChange(field.id, value)
                                }
                            }
                        )
                    }
                    if (editor.plugin.configurationSchema.prerequisites.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "Prerequisites",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        editor.plugin.configurationSchema.prerequisites.forEach { prerequisite ->
                            Text(
                                text = "• ${prerequisite.name}: ${prerequisite.remediation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
                editor.validationResult?.let {
                    ConfigurationValidationSummary(result = it)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, enabled = !inProgress) {
                        Text("Close")
                    }
                    OutlinedButton(onClick = onValidate, enabled = !inProgress) {
                        Text("Validate")
                    }
                    Button(onClick = onSave, enabled = !inProgress) {
                        Text("Save configuration")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationFieldEditor(
    field: com.aitask.core.domain.plugin.PluginConfigurationField,
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (field.type) {
            PluginConfigurationFieldType.BOOLEAN -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(
                            checked = value.equals("true", ignoreCase = true),
                            onCheckedChange = { onValueChange(it.toString()) },
                            enabled = enabled
                        )
                        Column {
                            Text(field.label)
                            field.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            else -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(field.label + if (field.required) " *" else "") },
                    placeholder = field.placeholder?.let { { Text(it) } },
                    supportingText = {
                        Column {
                            field.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (field.type == PluginConfigurationFieldType.SELECT && field.options.isNotEmpty()) {
                                Text(
                                    text = "Options: ${field.options.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                            if (error != null) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    visualTransformation = if (field.type == PluginConfigurationFieldType.SECRET) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun ConfigurationValidationSummary(result: PluginConfigurationValidationResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Divider()
        Text(
            text = if (result.valid) "Configuration valid" else "Configuration needs attention",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (result.valid) HEALTHY_COLOR else ERROR_COLOR
        )
        result.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        if (result.fieldErrors.isNotEmpty()) {
            Text(
                text = "Field issues",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            result.fieldErrors.forEach { (fieldId, message) ->
                Text(
                    text = "$fieldId: $message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (result.prerequisiteResults.isNotEmpty()) {
            Text(
                text = "Prerequisite checks",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            result.prerequisiteResults.forEach { prerequisite ->
                PrerequisiteRow(prerequisite)
            }
        }
        if (result.ruleMessages.isNotEmpty()) {
            Text(
                text = "Validation rules",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            result.ruleMessages.forEach { ruleMessage ->
                Text(
                    text = "• $ruleMessage",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        result.lastKnownGoodSnapshot?.let { snapshot ->
            Text(
                text = "Last known good config updated at ${snapshot.updatedAtEpochMillis}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun PrerequisiteRow(result: PluginPrerequisiteResult) {
    val accent = if (result.satisfied) HEALTHY_COLOR else ERROR_COLOR
    Surface(
        color = accent.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = result.prerequisite.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodySmall
            )
            result.remediation?.let { remediation ->
                Text(
                    text = remediation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
    }
}
