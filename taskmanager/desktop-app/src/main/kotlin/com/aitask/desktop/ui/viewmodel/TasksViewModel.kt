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
import com.aitask.core.domain.service.IDEService

/**
 * ViewModel for managing tasks UI state and operations
 */
class TasksViewModel(
    private val getTasksUseCase: GetTasksUseCase = DependencyContainer.getTasksUseCase,
    private val createTaskUseCase: CreateTaskUseCase = DependencyContainer.createTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase = DependencyContainer.updateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase = DependencyContainer.deleteTaskUseCase,
    private val getProjectsUseCase: GetProjectsUseCase = DependencyContainer.getProjectsUseCase,
    private val generateWorkspaceUseCase: GenerateWorkspaceUseCase = DependencyContainer.generateWorkspaceUseCase,
    private val launchIDEUseCase: LaunchIDEUseCase = DependencyContainer.launchIDEUseCase,
    private val ideService: IDEService = DependencyContainer.ideService,
    private val repositoryRepository: com.aitask.core.domain.repository.RepositoryRepository = DependencyContainer.repositoryRepository,
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
        projectId: UUID
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateTaskRequest(
                title = title,
                description = description,
                taskType = taskType,
                projectId = projectId
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
            val request = UpdateTaskRequest(
                status = newStatus
            )

            val result = updateTaskUseCase(taskId, request)
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

    fun generateWorkspace(taskId: UUID) {
        uiState = uiState.copy(isGeneratingWorkspace = true, error = null)
        scope.launch {
            // Get the task to find the project ID
            val task = uiState.tasks.find { it.id == taskId }
            if (task == null) {
                uiState = uiState.copy(
                    isGeneratingWorkspace = false,
                    error = "Task not found"
                )
                return@launch
            }

            val request = com.aitask.core.domain.model.CreateWorkspaceRequest(
                taskId = taskId,
                projectId = task.projectId,
                selectedRepositories = emptyList() // Use all repositories
            )

            val result = generateWorkspaceUseCase(request)
            result.fold(
                onSuccess = { workspace ->
                    uiState = uiState.copy(
                        isGeneratingWorkspace = false,
                        workspaceGenerationSuccess = "Workspace generated at: ${workspace.path}"
                    )
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isGeneratingWorkspace = false,
                        error = error.message ?: "Failed to generate workspace"
                    )
                }
            )
        }
    }

    fun launchIDE(taskId: UUID, ideType: IDEType) {
        uiState = uiState.copy(isLaunchingIDE = true, error = null)
        scope.launch {
            val request = LaunchIDERequest(
                taskId = taskId,
                ideType = ideType,
                updateTaskStatus = true
            )

            val result = launchIDEUseCase(request)
            result.fold(
                onSuccess = { response ->
                    uiState = uiState.copy(
                        isLaunchingIDE = false,
                        ideLaunchSuccess = "Launched ${ideType.name} successfully"
                    )
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLaunchingIDE = false,
                        error = error.message ?: "Failed to launch IDE"
                    )
                }
            )
        }
    }

    fun loadAvailableIDEs() {
        scope.launch {
            try {
                val ides = ideService.detectInstalledIDEs()
                uiState = uiState.copy(availableIDEs = ides)
            } catch (e: Exception) {
                // Silently fail - IDEs will be empty
            }
        }
    }

    fun loadProjectRepositories(projectId: UUID) {
        scope.launch {
            try {
                val repositories = repositoryRepository.findByProject(projectId)
                uiState = uiState.copy(projectRepositories = repositories)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun clearSuccessMessages() {
        uiState = uiState.copy(
            workspaceGenerationSuccess = null,
            ideLaunchSuccess = null
        )
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
    val error: String? = null,
    val isGeneratingWorkspace: Boolean = false,
    val isLaunchingIDE: Boolean = false,
    val workspaceGenerationSuccess: String? = null,
    val ideLaunchSuccess: String? = null,
    val availableIDEs: List<com.aitask.core.domain.model.InstalledIDE> = emptyList(),
    val projectRepositories: List<Repository> = emptyList()
)

