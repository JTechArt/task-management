package com.aitask.desktop.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.*
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun EmptyTasksState(
    hasProjects: Boolean,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Task,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = if (hasProjects) "No tasks yet" else "No projects yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = if (hasProjects) 
                    "Create your first task to get started" 
                else 
                    "Create a project first, then add tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (hasProjects) {
                Button(
                    onClick = onCreateClick,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Task")
                }
            }
        }
    }
}

@Composable
fun TaskFilters(
    projects: List<Project>,
    selectedProjectId: UUID?,
    selectedStatus: TaskStatus?,
    selectedTaskType: TaskType?,
    searchQuery: String,
    onProjectSelected: (UUID?) -> Unit,
    onStatusSelected: (TaskStatus?) -> Unit,
    onTaskTypeSelected: (TaskType?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search tasks") },
            placeholder = { Text("Search by title or description...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var projectExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = projectExpanded,
                onExpandedChange = { projectExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = projects.find { it.id == selectedProjectId }?.name ?: "All Projects",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Project") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = projectExpanded,
                    onDismissRequest = { projectExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Projects") },
                        onClick = {
                            onProjectSelected(null)
                            projectExpanded = false
                        }
                    )
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.name) },
                            onClick = {
                                onProjectSelected(project.id)
                                projectExpanded = false
                            }
                        )
                    }
                }
            }
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedStatus?.name ?: "All Statuses",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Statuses") },
                        onClick = {
                            onStatusSelected(null)
                            statusExpanded = false
                        }
                    )
                    TaskStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name) },
                            onClick = {
                                onStatusSelected(status)
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
            var taskTypeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = taskTypeExpanded,
                onExpandedChange = { taskTypeExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedTaskType?.name?.replace("_", " ") ?: "All Types",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = taskTypeExpanded,
                    onDismissRequest = { taskTypeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Types") },
                        onClick = {
                            onTaskTypeSelected(null)
                            taskTypeExpanded = false
                        }
                    )
                    TaskType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name.replace("_", " ")) },
                            onClick = {
                                onTaskTypeSelected(type)
                                taskTypeExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: Task,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            TaskStatusBadge(task.status)

            // Task info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    TaskTypeBadge(task.taskType)
                }

                task.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }

                Text(
                    text = "Created ${formatDate(task.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TaskDetailView(
    task: Task,
    projectMethodology: Methodology,
    onDelete: (UUID) -> Unit,
    onStatusChange: (UUID, TaskStatus) -> Unit,
    onSaveMethodologyOverride: (UUID, Methodology?) -> Unit = { _, _ -> },
    onGenerateWorkspace: ((UUID) -> Unit)? = null,
    onLaunchIDE: ((UUID, IDEType) -> Unit)? = null,
    onCleanupWorkspace: ((Task) -> Unit)? = null,
    availableIDEs: List<com.aitask.core.domain.model.InstalledIDE> = emptyList(),
    preferredIDEs: List<IDEType> = emptyList(),
    isGeneratingWorkspace: Boolean = false,
    workspaceGenerationProgress: String? = null,
    isLaunchingIDE: Boolean = false,
    isCleaningUpWorkspace: Boolean = false,
    modifier: Modifier = Modifier
) {
    var methodologyExpanded by remember { mutableStateOf(false) }
    var selectedMethodologyOverride by remember(task.methodologyOverride) { mutableStateOf(task.methodologyOverride) }
    LaunchedEffect(task.methodologyOverride) {
        selectedMethodologyOverride = task.methodologyOverride
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with delete button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onDelete(task.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete task")
            }
        }

        // Badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TaskStatusBadge(task.status)
            TaskTypeBadge(task.taskType)
        }

        // Description
        task.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Divider()

        // Task details
        DetailRow("Created", formatDate(task.createdAt))
        DetailRow("Updated", formatDate(task.updatedAt))
        DetailRow("Effective Methodology", task.effectiveMethodology(projectMethodology).name)
        DetailRow("Methodology Override", task.methodologyOverride?.name ?: "Project Default")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ExposedDropdownMenuBox(
                expanded = methodologyExpanded,
                onExpandedChange = { methodologyExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedMethodologyOverride?.name ?: "Project Default",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Task Methodology") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodologyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    supportingText = { Text("Effective: ${(selectedMethodologyOverride ?: projectMethodology).name}") }
                )
                ExposedDropdownMenu(
                    expanded = methodologyExpanded,
                    onDismissRequest = { methodologyExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Project Default") },
                        onClick = {
                            selectedMethodologyOverride = null
                            methodologyExpanded = false
                        }
                    )
                    Methodology.values().forEach { methodology ->
                        DropdownMenuItem(
                            text = { Text(methodology.name) },
                            onClick = {
                                selectedMethodologyOverride = methodology
                                methodologyExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                onClick = { onSaveMethodologyOverride(task.id, selectedMethodologyOverride) },
                enabled = selectedMethodologyOverride != task.methodologyOverride
            ) {
                Text("Save")
            }
        }

        task.completedAt?.let { completed ->
            DetailRow("Completed", formatDate(completed))
        }

        task.workspacePath?.let { workspace ->
            DetailRow("Workspace", workspace)
        }

        task.branchName?.let { branch ->
            DetailRow("Branch", branch)
        }

        if (task.isCompleted) {
            val workspaceStateText = when {
                task.workspaceCleanedAt != null -> "Cleanup completed"
                task.workspacePath != null -> "Retained"
                else -> "Not created"
            }
            DetailRow("Workspace state", workspaceStateText)
        }

        Divider()

        // Workspace section
        Text(
            text = "Workspace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (task.workspacePath == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No workspace generated yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (onGenerateWorkspace != null) {
                        Button(
                            onClick = { onGenerateWorkspace(task.id) },
                            enabled = !isGeneratingWorkspace
                        ) {
                            if (isGeneratingWorkspace) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (isGeneratingWorkspace) "Generating..." else "Generate Workspace")
                        }
                    }
                }

                // Show progress message if generating
                if (isGeneratingWorkspace && workspaceGenerationProgress != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = workspaceGenerationProgress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        } else {
            // IDE Launch section
            Text(
                text = "Launch IDE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (onLaunchIDE != null && preferredIDEs.isNotEmpty()) {
                val installedPreferredIDEs = availableIDEs.filter { ide ->
                    preferredIDEs.contains(ide.type)
                }

                if (installedPreferredIDEs.isEmpty()) {
                    Text(
                        text = "No preferred IDEs installed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        installedPreferredIDEs.forEach { ide ->
                            Button(
                                onClick = { onLaunchIDE(task.id, ide.type) },
                                enabled = !isLaunchingIDE,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLaunchingIDE) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Launch ${ide.type.name}")
                                }
                            }
                        }
                    }
                }
            }

            if (task.isCompleted && task.workspacePath != null && task.workspaceCleanedAt == null && onCleanupWorkspace != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Workspace Cleanup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { onCleanupWorkspace(task) },
                    enabled = !isCleaningUpWorkspace,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isCleaningUpWorkspace) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isCleaningUpWorkspace) "Cleaning up..." else "Clean up workspace")
                }
            }
        }

        Divider()

        // Status change buttons
        Text(
            text = "Change Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskStatus.values().forEach { status ->
                if (status != task.status) {
                    OutlinedButton(
                        onClick = { onStatusChange(task.id, status) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(status.name, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DestructiveCleanupConfirmationDialog(
    taskTitle: String,
    typedText: String,
    onTypedTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Clean up workspace?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This will permanently delete local workspace data for task \"$taskTitle\". This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Type DELETE to continue:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = typedText,
                    onValueChange = onTypedTextChange,
                    placeholder = { Text("DELETE") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        if (typedText.isNotEmpty() && typedText != "DELETE") {
                            Text("Must type DELETE exactly", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = typedText == "DELETE",
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clean up workspace")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TaskStatusBadge(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.PENDING -> MaterialTheme.colorScheme.secondary
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
        TaskStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        TaskStatus.ARCHIVED -> MaterialTheme.colorScheme.error
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TaskTypeBadge(type: TaskType) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatDate(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    return formatter.format(instant.atZone(java.time.ZoneId.systemDefault()))
}
