package com.aitask.desktop.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.*
import com.aitask.desktop.ui.projects.BmadToolSelectionSection
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
    projectBmadToolIds: List<String>,
    onDelete: (UUID) -> Unit,
    onStatusChange: (UUID, TaskStatus) -> Unit,
    onSaveMethodologyOverride: (UUID, Methodology?) -> Unit = { _, _ -> },
    onSaveBmadToolOverride: (UUID, List<String>) -> Unit = { _, _ -> },
    onResetBmadToolOverride: (UUID) -> Unit = {},
    onSaveBmadInjectionOverride: (UUID, Boolean?) -> Unit = { _, _ -> },
    onSaveDescription: (UUID, String?) -> Unit = { _, _ -> },
    onGenerateWorkspace: ((UUID) -> Unit)? = null,
    onLaunchIDE: ((UUID, IDEType) -> Unit)? = null,
    onCleanupWorkspace: ((Task) -> Unit)? = null,
    onGenerateTaskContentSuggestion: ((UUID, String, String?, TaskType, TaskContentGenerationMode, UUID?) -> Unit)? = null,
    onClearTaskContentSuggestion: (() -> Unit)? = null,
    onGenerateGitAssistantSuggestion: ((UUID, GitAssistantSuggestionMode) -> Unit)? = null,
    onClearGitAssistantSuggestion: (() -> Unit)? = null,
    onUseGitAssistantSuggestion: ((UUID, GitAssistantSuggestionMode, String) -> Unit)? = null,
    availableIDEs: List<com.aitask.core.domain.model.InstalledIDE> = emptyList(),
    preferredIDEs: List<IDEType> = emptyList(),
    isGeneratingWorkspace: Boolean = false,
    workspaceGenerationProgress: String? = null,
    isLaunchingIDE: Boolean = false,
    isCleaningUpWorkspace: Boolean = false,
    isGeneratingTaskContent: Boolean = false,
    taskContentGenerationError: String? = null,
    taskContentSuggestion: String? = null,
    taskContentSuggestionTargetTaskId: UUID? = null,
    isGeneratingGitAssistantSuggestion: Boolean = false,
    gitAssistantSuggestionError: String? = null,
    gitAssistantSuggestion: String? = null,
    gitAssistantSuggestionTargetTaskId: UUID? = null,
    gitAssistantSuggestionMode: GitAssistantSuggestionMode? = null,
    modifier: Modifier = Modifier
) {
    var methodologyExpanded by remember { mutableStateOf(false) }
    var selectedMethodologyOverride by remember(task.methodologyOverride) { mutableStateOf(task.methodologyOverride) }
    var selectedBmadToolIds by remember(task.bmadToolOverrideIds, projectBmadToolIds) {
        mutableStateOf(
            task.effectiveBmadToolIds(projectBmadToolIds)
        )
    }
    var selectedInjectionOverride by remember(task.bmadInjectionEnabledOverride) {
        mutableStateOf(task.bmadInjectionEnabledOverride)
    }
    var descriptionDraft by remember(task.description) { mutableStateOf(task.description ?: "") }
    val scrollState = rememberScrollState()
    val taskContentSuggestionForCurrentTask = taskContentSuggestionTargetTaskId == task.id && taskContentSuggestion != null
    val gitAssistantSuggestionForCurrentTask = gitAssistantSuggestionTargetTaskId == task.id && gitAssistantSuggestion != null
    val clipboardManager = LocalClipboardManager.current
    LaunchedEffect(task.methodologyOverride) {
        selectedMethodologyOverride = task.methodologyOverride
    }
    LaunchedEffect(task.bmadToolOverrideIds, projectBmadToolIds) {
        selectedBmadToolIds = task.effectiveBmadToolIds(projectBmadToolIds)
    }
    LaunchedEffect(task.bmadInjectionEnabledOverride) {
        selectedInjectionOverride = task.bmadInjectionEnabledOverride
    }
    LaunchedEffect(task.description) {
        descriptionDraft = task.description ?: ""
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize()
            .verticalScroll(scrollState)
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

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Description editor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generate a description or summary, then edit it before saving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = descriptionDraft,
                    onValueChange = { descriptionDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    label = { Text("Task description / summary") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onGenerateTaskContentSuggestion?.invoke(
                                task.projectId,
                                task.title,
                                descriptionDraft.takeIf { it.isNotBlank() },
                                task.taskType,
                                TaskContentGenerationMode.DESCRIPTION,
                                task.id
                            )
                        },
                        enabled = !isGeneratingTaskContent && onGenerateTaskContentSuggestion != null
                    ) {
                        Text(if (isGeneratingTaskContent) "Generating..." else "Generate description")
                    }
                    OutlinedButton(
                        onClick = {
                            onGenerateTaskContentSuggestion?.invoke(
                                task.projectId,
                                task.title,
                                descriptionDraft.takeIf { it.isNotBlank() },
                                task.taskType,
                                TaskContentGenerationMode.SUMMARY,
                                task.id
                            )
                        },
                        enabled = !isGeneratingTaskContent && onGenerateTaskContentSuggestion != null
                    ) {
                        Text("Generate summary")
                    }
                }
                if (taskContentSuggestionForCurrentTask) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Generated suggestion",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = taskContentSuggestion!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    descriptionDraft = taskContentSuggestion!!
                                    onClearTaskContentSuggestion?.invoke()
                                }) {
                                    Text("Use suggestion")
                                }
                                OutlinedButton(onClick = { onClearTaskContentSuggestion?.invoke() }) {
                                    Text("Discard")
                                }
                            }
                        }
                    }
                }
                if (taskContentGenerationError != null && taskContentSuggestionTargetTaskId == task.id) {
                    Text(
                        text = taskContentGenerationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSaveDescription(task.id, descriptionDraft.trim().takeIf { it.isNotEmpty() }) },
                        enabled = descriptionDraft.trim() != (task.description ?: "")
                    ) {
                        Text("Save description")
                    }
                    OutlinedButton(
                        onClick = { descriptionDraft = task.description ?: "" },
                        enabled = descriptionDraft != (task.description ?: "")
                    ) {
                        Text("Revert")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Git assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Draft commit messages, PR descriptions, or comment text from the current task context.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onGenerateGitAssistantSuggestion?.invoke(task.id, GitAssistantSuggestionMode.COMMIT_MESSAGE)
                        },
                        enabled = !isGeneratingGitAssistantSuggestion && onGenerateGitAssistantSuggestion != null
                    ) {
                        Text(if (isGeneratingGitAssistantSuggestion && gitAssistantSuggestionMode == GitAssistantSuggestionMode.COMMIT_MESSAGE) "Generating..." else "Commit message")
                    }
                    OutlinedButton(
                        onClick = {
                            onGenerateGitAssistantSuggestion?.invoke(task.id, GitAssistantSuggestionMode.PR_DESCRIPTION)
                        },
                        enabled = !isGeneratingGitAssistantSuggestion && onGenerateGitAssistantSuggestion != null
                    ) {
                        Text("PR description")
                    }
                    OutlinedButton(
                        onClick = {
                            onGenerateGitAssistantSuggestion?.invoke(task.id, GitAssistantSuggestionMode.COMMENT_DRAFT)
                        },
                        enabled = !isGeneratingGitAssistantSuggestion && onGenerateGitAssistantSuggestion != null
                    ) {
                        Text("Comment draft")
                    }
                }
                if (gitAssistantSuggestionForCurrentTask) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "AI-generated ${gitAssistantSuggestionMode?.name?.lowercase()?.replace('_', ' ') ?: "suggestion"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = gitAssistantSuggestion!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    clipboardManager.setText(AnnotatedString(gitAssistantSuggestion!!))
                                    onUseGitAssistantSuggestion?.invoke(
                                        task.id,
                                        gitAssistantSuggestionMode ?: GitAssistantSuggestionMode.COMMIT_MESSAGE,
                                        gitAssistantSuggestion!!
                                    )
                                }) {
                                    Text("Use suggestion")
                                }
                                OutlinedButton(onClick = { onClearGitAssistantSuggestion?.invoke() }) {
                                    Text("Discard")
                                }
                            }
                        }
                    }
                }
                if (gitAssistantSuggestionError != null && gitAssistantSuggestionTargetTaskId == task.id) {
                    Text(
                        text = gitAssistantSuggestionError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Divider()

        // Task details
        DetailRow("Created", formatDate(task.createdAt))
        DetailRow("Updated", formatDate(task.updatedAt))
        DetailRow("Effective Methodology", task.effectiveMethodology(projectMethodology).name)
        DetailRow("Methodology Source", if (task.methodologyOverride != null) "Task Override" else "Project Default")
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
        if (task.effectiveMethodology(projectMethodology) == Methodology.BMAD) {
            DetailRow(
                "BMAD Tools Source",
                if (task.bmadToolOverrideIds != null) "Task Override" else "Project Default"
            )
            Text(
                text = "BMAD Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Task-specific override for the active BMAD tool set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            BmadToolSelectionSection(
                selectedToolIds = selectedBmadToolIds,
                onSelectionChange = { selectedBmadToolIds = it }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSaveBmadToolOverride(task.id, selectedBmadToolIds) },
                    enabled = selectedBmadToolIds != task.bmadToolOverrideIds
                ) {
                    Text("Save BMAD Tools")
                }
                if (task.bmadToolOverrideIds != null) {
                    OutlinedButton(onClick = { onResetBmadToolOverride(task.id) }) {
                        Text("Use Project Defaults")
                    }
                }
            }
            Text(
                text = "BMAD Injection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            DetailRow(
                "Injection Source",
                if (task.bmadInjectionEnabledOverride != null) "Task Override" else "Project Default"
            )
            Text(
                text = "Control whether BMAD files are injected for this task even when the project default is BMAD.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedInjectionOverride == null,
                    onClick = { selectedInjectionOverride = null },
                    label = { Text("Project Default") }
                )
                FilterChip(
                    selected = selectedInjectionOverride == true,
                    onClick = { selectedInjectionOverride = true },
                    label = { Text("Force On") }
                )
                FilterChip(
                    selected = selectedInjectionOverride == false,
                    onClick = { selectedInjectionOverride = false },
                    label = { Text("Force Off") }
                )
            }
            Button(
                onClick = { onSaveBmadInjectionOverride(task.id, selectedInjectionOverride) },
                enabled = selectedInjectionOverride != task.bmadInjectionEnabledOverride
            ) {
                Text("Save Injection Override")
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
