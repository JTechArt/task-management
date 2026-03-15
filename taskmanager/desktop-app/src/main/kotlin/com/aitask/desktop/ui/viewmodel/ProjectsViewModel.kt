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

/**
 * ViewModel for managing projects UI state and operations
 */
class ProjectsViewModel(
    private val getProjectsUseCase: GetProjectsUseCase = DependencyContainer.getProjectsUseCase,
    private val createProjectUseCase: CreateProjectUseCase = DependencyContainer.createProjectUseCase,
    private val updateProjectUseCase: UpdateProjectUseCase = DependencyContainer.updateProjectUseCase,
    private val archiveProjectUseCase: ArchiveProjectUseCase = DependencyContainer.archiveProjectUseCase,
    private val createRepositoryUseCase: CreateRepositoryUseCase = DependencyContainer.createRepositoryUseCase,
    private val updateRepositoryUseCase: UpdateRepositoryUseCase = DependencyContainer.updateRepositoryUseCase,
    private val deleteRepositoryUseCase: DeleteRepositoryUseCase = DependencyContainer.deleteRepositoryUseCase,
    private val repositoryRepository: com.aitask.core.domain.repository.RepositoryRepository = DependencyContainer.repositoryRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    var uiState by mutableStateOf(ProjectsUiState())
        private set
    
    init {
        loadProjects()
    }
    
    fun loadProjects() {
        uiState = uiState.copy(isLoading = true, error = null)
        scope.launch {
            val result = getProjectsUseCase(includeArchived = false)
            result.fold(
                onSuccess = { projects ->
                    uiState = uiState.copy(
                        projects = projects,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load projects"
                    )
                }
            )
        }
    }
    
    fun createProject(
        name: String,
        description: String?,
        workspacePath: String,
        branchTemplate: String,
        repositoryName: String,
        cloneUrl: String,
        provider: GitProvider,
        authType: AuthType,
        preferredIDEs: List<IDEType>
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val projectRequest = CreateProjectRequest(
                name = name,
                description = description,
                workspacePath = workspacePath,
                branchTemplate = branchTemplate
            )
            
            val repositoryRequest = CreateRepositoryRequest(
                projectId = java.util.UUID.randomUUID(), // Will be replaced by use case
                name = repositoryName,
                cloneUrl = cloneUrl,
                provider = provider,
                authType = authType,
                preferredIDEs = preferredIDEs
            )
            
            val result = createProjectUseCase(projectRequest, repositoryRequest)
            result.fold(
                onSuccess = { (project, repository) ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showCreateDialog = false
                    )
                    loadProjects()
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to create project"
                    )
                }
            )
        }
    }
    
    fun selectProject(project: Project?) {
        uiState = uiState.copy(selectedProject = project)
        if (project != null) {
            loadProjectRepositories(project.id)
        }
    }
    
    private fun loadProjectRepositories(projectId: java.util.UUID) {
        scope.launch {
            try {
                val repositories = repositoryRepository.findByProject(projectId)
                uiState = uiState.copy(selectedProjectRepositories = repositories)
            } catch (e: Exception) {
                // Silently fail - repositories will be empty
            }
        }
    }
    
    fun archiveProject(projectId: java.util.UUID) {
        scope.launch {
            val result = archiveProjectUseCase(projectId)
            result.fold(
                onSuccess = {
                    loadProjects()
                    if (uiState.selectedProject?.id == projectId) {
                        uiState = uiState.copy(selectedProject = null)
                    }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to archive project"
                    )
                }
            )
        }
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

    // Repository Management
    fun showAddRepositoryDialog() {
        uiState = uiState.copy(showAddRepositoryDialog = true, editingRepository = null, error = null)
    }

    fun showEditRepositoryDialog(repository: Repository) {
        uiState = uiState.copy(showAddRepositoryDialog = true, editingRepository = repository, error = null)
    }

    fun hideRepositoryDialog() {
        uiState = uiState.copy(showAddRepositoryDialog = false, editingRepository = null, error = null)
    }

    fun createRepository(
        projectId: java.util.UUID,
        name: String,
        cloneUrl: String,
        provider: GitProvider,
        authType: AuthType,
        preferredIDEs: List<IDEType>,
        isPrimary: Boolean = false
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateRepositoryRequest(
                projectId = projectId,
                name = name,
                cloneUrl = cloneUrl,
                provider = provider,
                authType = authType,
                preferredIDEs = preferredIDEs,
                isPrimary = isPrimary
            )

            val result = createRepositoryUseCase(request)
            result.fold(
                onSuccess = { repository ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showAddRepositoryDialog = false
                    )
                    // Reload repositories for the current project
                    uiState.selectedProject?.let { loadProjectRepositories(it.id) }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to create repository"
                    )
                }
            )
        }
    }

    fun updateRepository(
        repositoryId: java.util.UUID,
        name: String?,
        cloneUrl: String?,
        provider: GitProvider?,
        authType: AuthType?,
        preferredIDEs: List<IDEType>?,
        isPrimary: Boolean?
    ) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = UpdateRepositoryRequest(
                name = name,
                cloneUrl = cloneUrl,
                provider = provider,
                authType = authType,
                preferredIDEs = preferredIDEs,
                isPrimary = isPrimary
            )

            val result = updateRepositoryUseCase(repositoryId, request)
            result.fold(
                onSuccess = { repository ->
                    uiState = uiState.copy(
                        isSaving = false,
                        showAddRepositoryDialog = false,
                        editingRepository = null
                    )
                    // Reload repositories for the current project
                    uiState.selectedProject?.let { loadProjectRepositories(it.id) }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        error = error.message ?: "Failed to update repository"
                    )
                }
            )
        }
    }

    fun deleteRepository(repositoryId: java.util.UUID) {
        scope.launch {
            val result = deleteRepositoryUseCase(repositoryId)
            result.fold(
                onSuccess = {
                    // Reload repositories for the current project
                    uiState.selectedProject?.let { loadProjectRepositories(it.id) }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to delete repository"
                    )
                }
            )
        }
    }
}

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val selectedProject: Project? = null,
    val selectedProjectRepositories: List<Repository> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showAddRepositoryDialog: Boolean = false,
    val editingRepository: Repository? = null,
    val error: String? = null
)

