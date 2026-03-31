package com.aitask.desktop.ui.slack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.service.SlackAnalysisTrigger
import com.aitask.desktop.ui.plugins.PluginConfigurationFormContent
import com.aitask.desktop.ui.viewmodel.SlackAnalyzerViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SlackAnalyzerView(
    viewModel: SlackAnalyzerViewModel = remember { SlackAnalyzerViewModel() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.load()
    }
    val uiState = viewModel.uiState
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Slack Analyzer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        item {
            Text(
                text = "Source & schedule",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = "Choose which channels feed analysis and whether runs are manual, scheduled, or both. Validation checks Slack connectivity and channel access before saving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        item {
            uiState.editor?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Incremental analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Runs fetch only new messages since the last successful scan per channel. One channel failing does not stop the rest.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = viewModel::runAnalysis,
                                enabled = !uiState.configurationInProgress && !uiState.analysisRunInProgress
                            ) {
                                Text("Run analysis now")
                            }
                            if (uiState.analysisRunInProgress) {
                                LinearProgressIndicator(modifier = Modifier.weight(1f).height(8.dp))
                            }
                        }
                        if (uiState.analysisRunInProgress) {
                            Text(
                                text = "Run in progress",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        uiState.analysisChannelProgress?.let { progress ->
                            progress.channelBeingProcessed?.let { ch ->
                                Text(
                                    text = "Processing: $ch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (progress.pendingChannelIds.isNotEmpty()) {
                                Text(
                                    text = "Queued: ${progress.pendingChannelIds.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                        uiState.analysisProgressMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        uiState.analysisLastSummary?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                        uiState.analysisLastError?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Run history & status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = "Completed runs are recorded as activity events. Filter by date, outcome, or trigger (manual vs scheduled).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        item {
            SlackRunHistoryFiltersCard(
                dateFrom = uiState.runHistoryFilters.dateFrom,
                dateTo = uiState.runHistoryFilters.dateTo,
                selectedStatus = uiState.runHistoryFilters.status,
                selectedTrigger = uiState.runHistoryFilters.trigger,
                onDateFromChange = viewModel::setRunHistoryDateFrom,
                onDateToChange = viewModel::setRunHistoryDateTo,
                onStatusChange = viewModel::setRunHistoryStatusFilter,
                onTriggerChange = viewModel::setRunHistoryTriggerFilter,
                onClear = viewModel::clearRunHistoryFilters
            )
        }
        item {
            if (uiState.runHistoryLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.runHistory.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No analysis runs match your filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
        items(uiState.runHistory, key = { it.id }) { activity ->
            SlackAnalysisRunHistoryRow(activity = activity)
        }
        item {
            uiState.runHistoryError?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = err,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        item {
            uiState.feedback?.let { feedback ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (feedback.isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedback.message,
                        modifier = Modifier.padding(16.dp),
                        color = if (feedback.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
        }
        item {
            uiState.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                uiState.editor?.let { editor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        PluginConfigurationFormContent(
                            editor = editor,
                            inProgress = uiState.configurationInProgress,
                            title = "Slack Channel Analyzer",
                            includeCloseButton = false,
                            onDismiss = null,
                            onValidate = viewModel::validateConfiguration,
                            onSave = viewModel::saveConfiguration,
                            onScopeKeyChange = {},
                            onFieldChange = viewModel::updateConfigurationField,
                            onSecretChange = viewModel::updateConfigurationSecret,
                            onScheduleChange = viewModel::updateConfigurationSchedule,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlackRunHistoryFiltersCard(
    dateFrom: String,
    dateTo: String,
    selectedStatus: ActivityStatus?,
    selectedTrigger: SlackAnalysisTrigger?,
    onDateFromChange: (String) -> Unit,
    onDateToChange: (String) -> Unit,
    onStatusChange: (ActivityStatus?) -> Unit,
    onTriggerChange: (SlackAnalysisTrigger?) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStatus?.name?.replace('_', ' ') ?: "All outcomes",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Outcome") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All outcomes") },
                                onClick = {
                                    onStatusChange(null)
                                    expanded = false
                                }
                            )
                            ActivityStatus.entries.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.name.replace('_', ' ')) },
                                    onClick = {
                                        onStatusChange(st)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTrigger?.name?.replace('_', ' ') ?: "All triggers",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Trigger") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All triggers") },
                                onClick = {
                                    onTriggerChange(null)
                                    expanded = false
                                }
                            )
                            SlackAnalysisTrigger.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name.replace('_', ' ')) },
                                    onClick = {
                                        onTriggerChange(t)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dateFrom,
                    onValueChange = onDateFromChange,
                    label = { Text("From date") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateTo,
                    onValueChange = onDateToChange,
                    label = { Text("To date") },
                    placeholder = { Text("yyyy-MM-dd") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun SlackAnalysisRunHistoryRow(activity: Activity) {
    var expanded by remember(activity.id) { mutableStateOf(false) }
    val meta: Map<String, String> = activity.metadata
    val statusColor: Color = when (activity.status) {
        ActivityStatus.SUCCESS -> Color(0xFF2E7D32)
        ActivityStatus.FAILED -> Color(0xFFC62828)
        ActivityStatus.IN_PROGRESS -> Color(0xFF1565C0)
    }
    val processed: String = meta["processedCount"] ?: "—"
    val skipped: String = meta["skippedCount"] ?: "—"
    val failed: String = meta["failedCount"] ?: "—"
    val durationMs: Long? = meta["durationMs"]?.toLongOrNull()
    val trigger: String = meta["trigger"] ?: "—"
    val failures: String = meta["failures"].orEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatRunInstant(activity.createdAt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Trigger: ${trigger.replace('_', ' ')} · ${activity.status.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = durationMs?.let { formatDurationMs(it) } ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            Text(
                text = "Succeeded: $processed · Skipped: $skipped · Failed: $failed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            if (failures.isNotBlank()) {
                Text(
                    text = if (expanded) "Hide failure details" else "Show failure details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
            if (expanded && failures.isNotBlank()) {
                Text(
                    text = failures,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatRunInstant(instant: Instant): String {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}

private fun formatDurationMs(ms: Long): String {
    if (ms < 1000L) {
        return "${ms}ms"
    }
    val secs: Long = ms / 1000L
    if (secs < 60L) {
        return "${secs}s"
    }
    val mins: Long = secs / 60L
    val rem: Long = secs % 60L
    return "${mins}m ${rem}s"
}
