package com.aitask.desktop.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.desktop.ui.viewmodel.TasksViewModel
import java.util.UUID

@Composable
fun TasksView(
    modifier: Modifier = Modifier,
    viewModel: TasksViewModel = remember { TasksViewModel() }
) {
    val uiState = viewModel.uiState
    
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
        TaskFilters(
            projects = uiState.projects,
            selectedProjectId = uiState.selectedProjectFilter,
            selectedStatus = uiState.selectedStatusFilter,
            onProjectSelected = { viewModel.setProjectFilter(it) },
            onStatusSelected = { viewModel.setStatusFilter(it) }
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
        
        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.tasks.isEmpty()) {
            // Empty state
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
                        items(uiState.tasks) { task ->
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
                        TaskDetailView(
                            task = uiState.selectedTask,
                            onDelete = { viewModel.deleteTask(it) },
                            onStatusChange = { taskId, status ->
                                viewModel.updateTaskStatus(taskId, status)
                            }
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
            onConfirm = { title, description, taskType, projectId, priority ->
                viewModel.createTask(title, description, taskType, projectId, priority)
            },
            isSaving = uiState.isSaving
        )
    }
}

