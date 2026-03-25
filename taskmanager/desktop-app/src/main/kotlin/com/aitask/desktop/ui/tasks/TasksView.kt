package com.aitask.desktop.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.desktop.ui.viewmodel.TasksViewModel
import java.util.UUID

@Composable
fun TasksView(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = remember { TasksViewModel() },
    taskIdToSelectOnMount: UUID? = null,
    onMountedAfterSelection: (() -> Unit)? = null
) {
    val uiState = viewModel.uiState

    // Load available IDEs on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAvailableIDEs()
    }

    // Load project repositories when task is selected
    LaunchedEffect(uiState.selectedTask) {
        uiState.selectedTask?.let { task ->
            viewModel.loadProjectRepositories(task.projectId)
        }
    }

    // Pre-select task when navigating from dashboard
    LaunchedEffect(taskIdToSelectOnMount, uiState.tasks) {
        val taskId = taskIdToSelectOnMount ?: return@LaunchedEffect
        if (uiState.tasks.isNotEmpty()) {
            val task = uiState.tasks.find { it.id == taskId }
            if (task != null) {
                viewModel.selectTask(task)
            }
            onMountedAfterSelection?.invoke()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with create button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage development tasks and track progress",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { /* Show filters */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter tasks")
                }
                Button(
                    onClick = { viewModel.showCreateDialog() },
                    enabled = !uiState.isLoading && uiState.projects.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Task")
                }
            }
        }
        
        // Filters
        val filteredTasks = remember(
            uiState.tasks,
            uiState.selectedTaskTypeFilter,
            uiState.searchQuery
        ) {
            uiState.tasks.filter { task ->
                val typeMatch = uiState.selectedTaskTypeFilter == null ||
                    task.taskType == uiState.selectedTaskTypeFilter
                val query = uiState.searchQuery.trim().lowercase()
                val searchMatch = query.isEmpty() ||
                    task.title.lowercase().contains(query) ||
                    (task.description?.lowercase()?.contains(query) == true)
                typeMatch && searchMatch
            }
        }
        TaskFilters(
            projects = uiState.projects,
            selectedProjectId = uiState.selectedProjectFilter,
            selectedStatus = uiState.selectedStatusFilter,
            selectedTaskType = uiState.selectedTaskTypeFilter,
            searchQuery = uiState.searchQuery,
            onProjectSelected = { viewModel.setProjectFilter(it) },
            onStatusSelected = { viewModel.setStatusFilter(it) },
            onTaskTypeSelected = { viewModel.setTaskTypeFilter(it) },
            onSearchQueryChange = { viewModel.setSearchQuery(it) }
        )
        
        // Error message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Success messages
        uiState.workspaceGenerationSuccess?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearSuccessMessages() }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        uiState.ideLaunchSuccess?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearSuccessMessages() }) {
                        Text("Dismiss")
                    }
                }
            }
        }
        
        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredTasks.isEmpty()) {
            // Empty state (no tasks or no matches)
            EmptyTasksState(
                hasProjects = uiState.projects.isNotEmpty(),
                onCreateClick = { viewModel.showCreateDialog() }
            )
        } else {
            // Tasks list
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tasks list (left side)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTasks) { task ->
                            TaskListItem(
                                task = task,
                                isSelected = task.id == uiState.selectedTask?.id,
                                onClick = { viewModel.selectTask(task) },
                                onStatusChange = { newStatus ->
                                    viewModel.updateTaskStatus(task.id, newStatus)
                                }
                            )
                        }
                    }
                }
                
                // Task detail (right side)
                if (uiState.selectedTask != null) {
                    Card(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        val selectedProject = uiState.projects.find { it.id == uiState.selectedTask.projectId }
                        TaskDetailView(
                            task = uiState.selectedTask,
                            projectMethodology = selectedProject?.methodology ?: Methodology.NONE,
                            projectBmadToolIds = selectedProject?.bmadToolIds ?: emptyList(),
                            onDelete = { viewModel.deleteTask(it) },
                            onStatusChange = { taskId, status ->
                                viewModel.updateTaskStatus(taskId, status)
                            },
                            onSaveMethodologyOverride = { taskId, methodologyOverride ->
                                viewModel.updateTaskMethodology(taskId, methodologyOverride)
                            },
                            onSaveBmadToolOverride = { taskId, toolIds ->
                                viewModel.updateTaskBmadTools(taskId, toolIds)
                            },
                            onResetBmadToolOverride = { taskId ->
                                viewModel.clearTaskBmadToolOverride(taskId)
                            },
                            onSaveBmadInjectionOverride = { taskId, enabled ->
                                viewModel.updateTaskBmadInjectionOverride(taskId, enabled)
                            },
                            onSaveDescription = { taskId, description ->
                                viewModel.updateTaskDescription(taskId, description)
                            },
                            onGenerateWorkspace = { taskId ->
                                viewModel.showRepositorySelectionDialog(taskId)
                            },
                            onLaunchIDE = { taskId, ideType ->
                                viewModel.launchIDE(taskId, ideType)
                            },
                            onCleanupWorkspace = { task -> viewModel.showCleanupConfirmationDialog(task) },
                            onGenerateTaskContentSuggestion = { projectId, title, currentDescription, taskType, mode, targetTaskId ->
                                viewModel.generateTaskContentSuggestion(
                                    projectId = projectId,
                                    title = title,
                                    currentDescription = currentDescription,
                                    taskType = taskType,
                                    mode = mode,
                                    targetTaskId = targetTaskId
                                )
                            },
                            onClearTaskContentSuggestion = { viewModel.clearTaskContentSuggestion() },
                            availableIDEs = uiState.availableIDEs,
                            preferredIDEs = uiState.projectRepositories.flatMap { it.preferredIDEs }.distinct(),
                            isGeneratingWorkspace = uiState.isGeneratingWorkspace,
                            workspaceGenerationProgress = uiState.workspaceGenerationProgress,
                            isLaunchingIDE = uiState.isLaunchingIDE,
                            isCleaningUpWorkspace = uiState.isCleaningUpWorkspace,
                            isGeneratingTaskContent = uiState.isGeneratingTaskContent,
                            taskContentGenerationError = uiState.taskContentGenerationError,
                            taskContentSuggestion = uiState.taskContentSuggestion,
                            taskContentSuggestionTargetTaskId = uiState.taskContentGenerationTargetTaskId
                        )
                    }
                }
            }
        }
    }
    
    // Create task dialog
    if (uiState.showCreateDialog) {
        CreateTaskDialog(
            projects = uiState.projects,
            onDismiss = { viewModel.hideCreateDialog() },
            viewModel = viewModel,
            onConfirm = { title, description, taskType, projectId, methodologyOverride ->
                viewModel.createTask(title, description, taskType, projectId, methodologyOverride)
            },
            isSaving = uiState.isSaving
        )
    }

    // Destructive cleanup confirmation dialog
    if (uiState.showCleanupConfirmationDialog && uiState.taskToCleanup != null) {
        DestructiveCleanupConfirmationDialog(
            taskTitle = uiState.taskToCleanup!!.title,
            typedText = uiState.cleanupConfirmationTypedText,
            onTypedTextChange = { viewModel.setCleanupConfirmationTypedText(it) },
            onConfirm = { viewModel.confirmCleanupWorkspace() },
            onDismiss = { viewModel.hideCleanupConfirmationDialog() }
        )
    }

    // Repository selection dialog
    if (uiState.showRepositorySelectionDialog && uiState.selectedTaskForWorkspace != null) {
        RepositorySelectionDialog(
            repositories = uiState.availableRepositoriesForSelection,
            defaultSelection = uiState.defaultRepositorySelection,
            onDismiss = { viewModel.hideRepositorySelectionDialog() },
            onConfirm = { selectedRepositoryIds ->
                viewModel.generateWorkspace(
                    uiState.selectedTaskForWorkspace.id,
                    selectedRepositoryIds
                )
            },
            isGenerating = uiState.isGeneratingWorkspace,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }
}
