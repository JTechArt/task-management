package com.aitask.desktop.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.desktop.ui.viewmodel.ActivityViewModel
import java.time.Instant

@Composable
fun ActivityView(
    databaseConnected: Boolean = true,
    onNavigateToTask: (java.util.UUID) -> Unit = {},
    viewModel: ActivityViewModel = remember { ActivityViewModel() }
) {
    LaunchedEffect(databaseConnected) {
        if (databaseConnected) viewModel.loadActivities()
    }
    val uiState = viewModel.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Activity history",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Review recent events: task creation, workspace preparation, IDE launch, Git preparation, and rule application.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(20.dp))
        ActivityFiltersSection(
            selectedProjectId = uiState.filters.projectId,
            selectedTaskId = uiState.filters.taskId,
            selectedType = uiState.filters.type,
            projects = uiState.projects,
            tasks = uiState.tasks,
            onProjectFilterChange = { viewModel.setProjectFilter(it) },
            onTaskFilterChange = { viewModel.setTaskFilter(it) },
            onTypeFilterChange = { viewModel.setTypeFilter(it) },
            onClearFilters = { viewModel.clearFilters() }
        )
        Spacer(Modifier.height(16.dp))
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.activities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No activity entries match your filters.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.activities) { activity ->
                    ActivityRow(activity = activity)
                }
            }
        }
        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun ActivityFiltersSection(
    selectedProjectId: java.util.UUID?,
    selectedTaskId: java.util.UUID?,
    selectedType: com.aitask.core.domain.model.ActivityType?,
    projects: List<com.aitask.core.domain.model.Project>,
    tasks: List<com.aitask.core.domain.model.Task>,
    onProjectFilterChange: (java.util.UUID?) -> Unit,
    onTaskFilterChange: (java.util.UUID?) -> Unit,
    onTypeFilterChange: (com.aitask.core.domain.model.ActivityType?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
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
                            value = selectedProjectId?.let { id ->
                                projects.find { it.id == id }?.name ?: "All projects"
                            } ?: "All projects",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Project") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All projects") },
                                onClick = {
                                    onProjectFilterChange(null)
                                    expanded = false
                                }
                            )
                            projects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.name) },
                                    onClick = {
                                        onProjectFilterChange(project.id)
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
                            value = selectedTaskId?.let { id ->
                                tasks.find { it.id == id }?.title ?: "All tasks"
                            } ?: "All tasks",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Task") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All tasks") },
                                onClick = {
                                    onTaskFilterChange(null)
                                    expanded = false
                                }
                            )
                            tasks.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text(task.title) },
                                    onClick = {
                                        onTaskFilterChange(task.id)
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
                            value = selectedType?.name?.replace('_', ' ') ?: "All types",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Event type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All types") },
                                onClick = {
                                    onTypeFilterChange(null)
                                    expanded = false
                                }
                            )
                            com.aitask.core.domain.model.ActivityType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.replace('_', ' ')) },
                                    onClick = {
                                        onTypeFilterChange(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: Activity) {
    val statusColor = when (activity.status) {
        ActivityStatus.SUCCESS -> Color(0xFF2E7D32)
        ActivityStatus.FAILED -> Color(0xFFC62828)
        ActivityStatus.IN_PROGRESS -> Color(0xFF1565C0)
    }
    val backgroundColor = when (activity.status) {
        ActivityStatus.SUCCESS -> statusColor.copy(alpha = 0.08f)
        ActivityStatus.FAILED -> statusColor.copy(alpha = 0.12f)
        ActivityStatus.IN_PROGRESS -> statusColor.copy(alpha = 0.08f)
    }
    val borderColor = when (activity.status) {
        ActivityStatus.SUCCESS -> statusColor.copy(alpha = 0.3f)
        ActivityStatus.FAILED -> statusColor.copy(alpha = 0.4f)
        ActivityStatus.IN_PROGRESS -> statusColor.copy(alpha = 0.3f)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = activity.status.name.take(1).uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = activity.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = formatRelativeTime(activity.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatRelativeTime(instant: Instant): String {
    val ago = java.time.Duration.between(instant, Instant.now())
    return when {
        ago.toMinutes() < 1 -> "Just now"
        ago.toMinutes() < 60 -> "${ago.toMinutes()}m ago"
        ago.toHours() < 24 -> "${ago.toHours()}h ago"
        else -> "${ago.toDays()}d ago"
    }
}
