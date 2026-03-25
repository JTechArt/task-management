package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.SlackNotificationService
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
    private val generateWorkspaceUseCase: GenerateWorkspaceUseCase = DependencyContainer.generateWorkspaceUseCase,
    private val cleanupWorkspaceUseCase: CleanupWorkspaceUseCase = DependencyContainer.cleanupWorkspaceUseCase,
    private val launchIDEUseCase: LaunchIDEUseCase = DependencyContainer.launchIDEUseCase,
    private val generateTaskContentUseCase: GenerateTaskContentUseCase = DependencyContainer.generateTaskContentUseCase,
    private val generateGitAssistantSuggestionUseCase: GenerateGitAssistantSuggestionUseCase = DependencyContainer.generateGitAssistantSuggestionUseCase,
    private val ideService: IDEService = DependencyContainer.ideService,
    private val repositoryRepository: com.aitask.core.domain.repository.RepositoryRepository = DependencyContainer.repositoryRepository,
    private val projectRepository: com.aitask.core.domain.repository.ProjectRepository = DependencyContainer.projectRepository,
    private val slackNotificationService: com.aitask.core.domain.service.SlackNotificationService = DependencyContainer.slackNotificationService,
    private val activityRepository: ActivityRepository = DependencyContainer.activityRepository,
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
        methodologyOverride: Methodology?
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateTaskRequest(
                title = title,
                description = description,
                taskType = taskType,
                projectId = projectId,
                methodologyOverride = methodologyOverride
            )

            val result = createTaskUseCase(request)
            result.fold(
                onSuccess = { task ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showCreateDialog = false
                    )
                    loadTasks()
                    projectRepository.findById(task.projectId)?.let { project ->
                        slackNotificationService.notifyInBackground(TaskEvent.TASK_CREATED, task, project)
                    }
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
                onSuccess = { updatedTask ->
                    loadTasks()
                    when (newStatus) {
                        TaskStatus.IN_PROGRESS -> TaskEvent.TASK_STARTED
                        TaskStatus.COMPLETED -> TaskEvent.TASK_COMPLETED
                        else -> null
                    }?.let { event ->
                        projectRepository.findById(updatedTask.projectId)?.let { project ->
                            slackNotificationService.notifyInBackground(event, updatedTask, project)
                        }
                    }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update task"
                    )
                }
            )
        }
    }

    fun updateTaskMethodology(taskId: UUID, methodologyOverride: Methodology?) {
        scope.launch {
            val result = updateTaskUseCase(
                taskId,
                UpdateTaskRequest(
                    methodologyOverride = methodologyOverride,
                    clearMethodologyOverride = methodologyOverride == null
                )
            )
            result.fold(
                onSuccess = { updatedTask ->
                    uiState = uiState.copy(selectedTask = updatedTask)
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update task methodology"
                    )
                }
            )
        }
    }

    fun updateTaskBmadTools(taskId: UUID, toolIds: List<String>) {
        scope.launch {
            val result = updateTaskUseCase(taskId, UpdateTaskRequest(bmadToolOverrideIds = toolIds))
            result.fold(
                onSuccess = { updatedTask ->
                    uiState = uiState.copy(selectedTask = updatedTask)
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update BMAD tools"
                    )
                }
            )
        }
    }

    fun clearTaskBmadToolOverride(taskId: UUID) {
        scope.launch {
            val result = updateTaskUseCase(taskId, UpdateTaskRequest(clearBmadToolOverrideIds = true))
            result.fold(
                onSuccess = { updatedTask ->
                    uiState = uiState.copy(selectedTask = updatedTask)
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to reset BMAD tools"
                    )
                }
            )
        }
    }

    fun updateTaskBmadInjectionOverride(taskId: UUID, enabled: Boolean?) {
        scope.launch {
            val result = updateTaskUseCase(
                taskId,
                UpdateTaskRequest(
                    bmadInjectionEnabledOverride = enabled,
                    clearBmadInjectionEnabledOverride = enabled == null
                )
            )
            result.fold(
                onSuccess = { updatedTask ->
                    uiState = uiState.copy(selectedTask = updatedTask)
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update BMAD injection override"
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
                    loadProjects()
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
        uiState = uiState.copy(selectedTask = task, error = null)

        // Load repositories for the selected task's project
        if (task != null) {
            scope.launch {
                val repositories = DependencyContainer.repositoryRepository.findByProject(task.projectId)
                uiState = uiState.copy(projectRepositories = repositories)
            }
        } else {
            uiState = uiState.copy(projectRepositories = emptyList())
        }
    }

    fun showRepositorySelectionDialog(taskId: UUID) {
        val task = uiState.tasks.find { it.id == taskId }
        if (task == null) {
            uiState = uiState.copy(error = "Task not found")
            return
        }

        scope.launch {
            val repositories = DependencyContainer.repositoryRepository.findByProject(task.projectId)

            // Determine default selection
            val defaultSelection = if (repositories.isNotEmpty()) {
                val primary = repositories.find { it.isPrimary }
                if (primary != null) {
                    listOf(primary.id)
                } else {
                    repositories.map { it.id }
                }
            } else {
                emptyList()
            }

            uiState = uiState.copy(
                showRepositorySelectionDialog = true,
                selectedTaskForWorkspace = task,
                availableRepositoriesForSelection = repositories,
                defaultRepositorySelection = defaultSelection,
                error = null
            )
        }
    }

    fun hideRepositorySelectionDialog() {
        uiState = uiState.copy(
            showRepositorySelectionDialog = false,
            selectedTaskForWorkspace = null,
            availableRepositoriesForSelection = emptyList(),
            defaultRepositorySelection = emptyList(),
            error = null
        )
    }

    fun setProjectFilter(projectId: UUID?) {
        uiState = uiState.copy(selectedProjectFilter = projectId)
        loadTasks()
    }
    
    fun setStatusFilter(status: TaskStatus?) {
        uiState = uiState.copy(selectedStatusFilter = status)
        loadTasks()
    }

    fun setTaskTypeFilter(taskType: TaskType?) {
        uiState = uiState.copy(selectedTaskTypeFilter = taskType)
    }

    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
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

    fun generateWorkspace(taskId: UUID, selectedRepositoryIds: List<UUID>) {
        uiState = uiState.copy(
            isGeneratingWorkspace = true,
            showRepositorySelectionDialog = false,
            error = null
        )
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
                selectedRepositories = selectedRepositoryIds
            )

            val result = generateWorkspaceUseCase(request) { progress ->
                // Update progress message
                uiState = uiState.copy(workspaceGenerationProgress = progress)
            }

            result.fold(
                onSuccess = { workspace ->
                    uiState = uiState.copy(
                        isGeneratingWorkspace = false,
                        workspaceGenerationSuccess = "Workspace generated at: ${workspace.path}",
                        workspaceGenerationProgress = null
                    )
                    loadTasks()
                    task?.let { t ->
                        projectRepository.findById(t.projectId)?.let { project ->
                            slackNotificationService.notifyInBackground(TaskEvent.WORKSPACE_CREATED, t, project)
                        }
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message
                        ?: error.cause?.message
                        ?: error.javaClass.simpleName
                        ?: "Failed to generate workspace"
                    uiState = uiState.copy(
                        isGeneratingWorkspace = false,
                        error = "$errorMsg See logs/taskmanager.log for details.",
                        workspaceGenerationProgress = null
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
                    projectRepository.findById(response.task.projectId)?.let { project ->
                        slackNotificationService.notifyInBackground(TaskEvent.IDE_LAUNCHED, response.task, project)
                    }
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

    fun generateTaskContentSuggestion(
        projectId: UUID,
        title: String,
        currentDescription: String?,
        taskType: TaskType,
        mode: TaskContentGenerationMode,
        targetTaskId: UUID? = null
    ) {
        uiState = uiState.copy(
            isGeneratingTaskContent = true,
            taskContentGenerationError = null,
            taskContentSuggestion = null,
            taskContentGenerationTargetTaskId = targetTaskId,
            taskContentGenerationMode = mode
        )
        scope.launch {
            val result = generateTaskContentUseCase(
                TaskContentGenerationRequest(
                    projectId = projectId,
                    title = title,
                    currentDescription = currentDescription,
                    taskType = taskType,
                    mode = mode
                )
            )
            result.fold(
                onSuccess = { suggestion ->
                    uiState = uiState.copy(
                        isGeneratingTaskContent = false,
                        taskContentSuggestion = suggestion.generatedText,
                        taskContentGenerationError = null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isGeneratingTaskContent = false,
                        taskContentGenerationError = error.message ?: "Failed to generate task content"
                    )
                }
            )
        }
    }

    fun generateGitAssistantSuggestion(
        taskId: UUID,
        mode: GitAssistantSuggestionMode
    ) {
        uiState = uiState.copy(
            isGeneratingGitAssistantSuggestion = true,
            gitAssistantSuggestionError = null,
            gitAssistantSuggestion = null,
            gitAssistantSuggestionTargetTaskId = taskId,
            gitAssistantSuggestionMode = mode
        )
        scope.launch {
            val result = generateGitAssistantSuggestionUseCase(
                GenerateGitAssistantSuggestionRequest(
                    taskId = taskId,
                    mode = mode
                )
            )
            result.fold(
                onSuccess = { suggestion ->
                    uiState = uiState.copy(
                        isGeneratingGitAssistantSuggestion = false,
                        gitAssistantSuggestion = suggestion.generatedText,
                        gitAssistantSuggestionError = null
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isGeneratingGitAssistantSuggestion = false,
                        gitAssistantSuggestionError = error.message ?: "Failed to generate git suggestion"
                    )
                }
            )
        }
    }

    fun clearGitAssistantSuggestion() {
        uiState = uiState.copy(
            gitAssistantSuggestion = null,
            gitAssistantSuggestionError = null,
            gitAssistantSuggestionTargetTaskId = null,
            gitAssistantSuggestionMode = null,
            isGeneratingGitAssistantSuggestion = false
        )
    }

    fun markGitAssistantSuggestionUsed(
        taskId: UUID,
        mode: GitAssistantSuggestionMode,
        suggestion: String
    ) {
        scope.launch {
            val task = uiState.tasks.find { it.id == taskId } ?: return@launch
            runCatching {
                activityRepository.create(
                    Activity(
                        id = UUID.randomUUID(),
                        type = ActivityType.LLM_SUGGESTION_USED,
                        entityType = "task",
                        entityId = task.id,
                        description = "Used AI-generated ${mode.name.lowercase().replace('_', ' ')} suggestion for task: ${task.title}",
                        metadata = mapOf(
                            "mode" to mode.name,
                            "taskTitle" to task.title,
                            "suggestionLength" to suggestion.length.toString(),
                            "branchName" to (task.branchName ?: "")
                        ).filterValues { it.isNotBlank() },
                        createdAt = java.time.Instant.now(),
                        status = ActivityStatus.SUCCESS,
                        projectId = task.projectId
                    )
                )
            }
            clearGitAssistantSuggestion()
        }
    }

    fun clearTaskContentSuggestion() {
        uiState = uiState.copy(
            taskContentSuggestion = null,
            taskContentGenerationError = null,
            taskContentGenerationTargetTaskId = null,
            taskContentGenerationMode = null,
            isGeneratingTaskContent = false
        )
    }

    fun updateTaskDescription(taskId: UUID, description: String?) {
        scope.launch {
            val result = updateTaskUseCase(
                taskId,
                UpdateTaskRequest(description = description?.takeIf { it.isNotBlank() })
            )
            result.fold(
                onSuccess = { updatedTask ->
                    uiState = uiState.copy(selectedTask = updatedTask)
                    loadTasks()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to update task description"
                    )
                }
            )
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

    fun showCleanupConfirmationDialog(task: Task) {
        uiState = uiState.copy(
            showCleanupConfirmationDialog = true,
            taskToCleanup = task,
            cleanupConfirmationTypedText = "",
            error = null
        )
    }

    fun hideCleanupConfirmationDialog() {
        uiState = uiState.copy(
            showCleanupConfirmationDialog = false,
            taskToCleanup = null,
            cleanupConfirmationTypedText = ""
        )
    }

    fun setCleanupConfirmationTypedText(text: String) {
        uiState = uiState.copy(cleanupConfirmationTypedText = text)
    }

    fun confirmCleanupWorkspace() {
        val task = uiState.taskToCleanup ?: return
        if (uiState.cleanupConfirmationTypedText != "DELETE") return
        uiState = uiState.copy(isCleaningUpWorkspace = true, showCleanupConfirmationDialog = false)
        scope.launch {
            val result = cleanupWorkspaceUseCase(task.id)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(
                        isCleaningUpWorkspace = false,
                        taskToCleanup = null,
                        cleanupConfirmationTypedText = "",
                        workspaceGenerationSuccess = "Workspace cleaned up successfully"
                    )
                    loadTasks()
                    selectTask(uiState.tasks.find { it.id == task.id })
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isCleaningUpWorkspace = false,
                        taskToCleanup = null,
                        cleanupConfirmationTypedText = "",
                        error = error.message ?: "Failed to clean up workspace"
                    )
                    loadTasks()
                }
            )
        }
    }
}

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val projects: List<Project> = emptyList(),
    val selectedTask: Task? = null,
    val selectedProjectFilter: UUID? = null,
    val selectedStatusFilter: TaskStatus? = null,
    val selectedTaskTypeFilter: TaskType? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCreateDialog: Boolean = false,
    val error: String? = null,
    val isGeneratingWorkspace: Boolean = false,
    val isLaunchingIDE: Boolean = false,
    val workspaceGenerationSuccess: String? = null,
    val workspaceGenerationProgress: String? = null,
    val ideLaunchSuccess: String? = null,
    val availableIDEs: List<com.aitask.core.domain.model.InstalledIDE> = emptyList(),
    val projectRepositories: List<Repository> = emptyList(),
    val showRepositorySelectionDialog: Boolean = false,
    val selectedTaskForWorkspace: Task? = null,
    val availableRepositoriesForSelection: List<Repository> = emptyList(),
    val defaultRepositorySelection: List<UUID> = emptyList(),
    val showCleanupConfirmationDialog: Boolean = false,
    val taskToCleanup: Task? = null,
    val cleanupConfirmationTypedText: String = "",
    val isCleaningUpWorkspace: Boolean = false,
    val isGeneratingTaskContent: Boolean = false,
    val taskContentGenerationError: String? = null,
    val taskContentSuggestion: String? = null,
    val taskContentGenerationTargetTaskId: UUID? = null,
    val taskContentGenerationMode: TaskContentGenerationMode? = null,
    val isGeneratingGitAssistantSuggestion: Boolean = false,
    val gitAssistantSuggestionError: String? = null,
    val gitAssistantSuggestion: String? = null,
    val gitAssistantSuggestionTargetTaskId: UUID? = null,
    val gitAssistantSuggestionMode: GitAssistantSuggestionMode? = null
)
