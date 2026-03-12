package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.*
import com.aitask.core.domain.usecase.*
import com.aitask.desktop.di.DependencyContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for managing tasks UI state and operations
 */
class TasksViewModel(
    private val getTasksUseCase: GetTasksUseCase = DependencyContainer.getTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase = DependencyContainer.createTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase = DependencyContainer.updateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase = DependencyContainer.deleteTaskUseCase,
    private val getProjectsUseCase: GetProjectsUseCase = DependencyContainer.getProjectsUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var uiState by mutableStateOf(TasksUiState())
        private set
    
    init {
        loadProjects()
        loadTasks()
    }
    
    private fun loadProjects() {
        scope.launch {
            val result = getProjectsUseCase(includeArchived = false)
            result.fold(
                onSuccess = { projects ->
                    uiState = uiState.copy(projects = projects)
                },
                onFailure = { /* Silently fail - projects will be empty */ }
            )
        }
    }
    
    fun loadTasks() {
        uiState = uiState.copy(isLoading = true, error = null)
        scope.launch {
            val result = getTasksUseCase(
                projectId = uiState.selectedProjectFilter,
                status = uiState.selectedStatusFilter
            )
            result.fold(
                onSuccess = { tasks ->
                    uiState = uiState.copy(
                        tasks = tasks,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load tasks"
                    )
                }
            )
        }
    }
    
    fun createTask(
        title: String,
        description: String?,
        taskType: TaskType,
        projectId: UUID,
        priority: TaskPriority
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateTaskRequest(
                title = title,
                description = description,
                taskType = taskType,
                projectId = projectId,
                priority = priority
            )
            
            val result = createTaskUseCase(request)
            result.fold(
                onSuccess = { task ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showCreateDialog = false
                    )
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to create task"
                    )
                }
            )
        }
    }
    
    fun updateTaskStatus(taskId: UUID, newStatus: TaskStatus) {
        scope.launch {
            val task = uiState.tasks.find { it.id == taskId } ?: return@launch
            val request = UpdateTaskRequest(
                taskId = taskId,
                title = task.title,
                description = task.description,
                status = newStatus,
                priority = task.priority
            )
            
            val result = updateTaskUseCase(request)
            result.fold(
                onSuccess = { loadTasks() },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update task"
                    )
                }
            )
        }
    }
    
    fun deleteTask(taskId: UUID) {
        scope.launch {
            val result = deleteTaskUseCase(taskId)
            result.fold(
                onSuccess = {
                    loadTasks()
                    if (uiState.selectedTask?.id == taskId) {
                        uiState = uiState.copy(selectedTask = null)
                    }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to delete task"
                    )
                }
            )
        }
    }
    
    fun selectTask(task: Task?) {
        uiState = uiState.copy(selectedTask = task)
    }
    
    fun setProjectFilter(projectId: UUID?) {
        uiState = uiState.copy(selectedProjectFilter = projectId)
        loadTasks()
    }
    
    fun setStatusFilter(status: TaskStatus?) {
        uiState = uiState.copy(selectedStatusFilter = status)
        loadTasks()
    }

    fun showCreateDialog() {
        uiState = uiState.copy(showCreateDialog = true, error = null)
    }

    fun hideCreateDialog() {
        uiState = uiState.copy(showCreateDialog = false, error = null)
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val selectedTask: Task? = null,
    val selectedProjectFilter: UUID? = null,
    val selectedStatusFilter: TaskStatus? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCreateDialog: Boolean = false,
    val error: String? = null
)

