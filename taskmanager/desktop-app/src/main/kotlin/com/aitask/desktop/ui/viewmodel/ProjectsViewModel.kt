package com.aitask.desktop.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aitask.core.domain.model.*
import com.aitask.core.domain.usecase.*
import com.aitask.core.domain.model.TaskEvent
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
    private val unarchiveProjectUseCase: UnarchiveProjectUseCase = DependencyContainer.unarchiveProjectUseCase,
    private val createRepositoryUseCase: CreateRepositoryUseCase = DependencyContainer.createRepositoryUseCase,
    private val updateRepositoryUseCase: UpdateRepositoryUseCase = DependencyContainer.updateRepositoryUseCase,
    private val deleteRepositoryUseCase: DeleteRepositoryUseCase = DependencyContainer.deleteRepositoryUseCase,
    private val repositoryRepository: com.aitask.core.domain.repository.RepositoryRepository = DependencyContainer.repositoryRepository,
    private val preRunScriptRepository: com.aitask.core.domain.repository.PreRunScriptRepository = DependencyContainer.preRunScriptRepository,
    private val getSlackChannelsUseCase: GetSlackChannelsUseCase = DependencyContainer.getSlackChannelsUseCase,
    private val createSlackChannelUseCase: CreateSlackChannelUseCase = DependencyContainer.createSlackChannelUseCase,
    private val updateSlackChannelUseCase: UpdateSlackChannelUseCase = DependencyContainer.updateSlackChannelUseCase,
    private val deleteSlackChannelUseCase: DeleteSlackChannelUseCase = DependencyContainer.deleteSlackChannelUseCase,
    private val sendSlackTestMessageUseCase: SendSlackTestMessageUseCase = DependencyContainer.sendSlackTestMessageUseCase,
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
            val result = getProjectsUseCase(includeArchived = uiState.showArchivedProjects)
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
        retentionPolicy: com.aitask.core.domain.service.RetentionPolicy,
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
                branchTemplate = branchTemplate,
                retentionPolicy = retentionPolicy
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
        uiState = uiState.copy(selectedProject = project, selectedDetailTab = com.aitask.desktop.ui.projects.ProjectDetailTab.REPOSITORIES)
        if (project != null) {
            loadProjectRepositories(project.id)
            loadPreRunScripts(project.id)
            loadSlackChannels(project.id)
        } else {
            uiState = uiState.copy(
                selectedProjectSlackChannels = emptyList(),
                selectedProjectPreRunScripts = emptyList()
            )
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

    private fun loadSlackChannels(projectId: java.util.UUID) {
        scope.launch {
            try {
                val channels = getSlackChannelsUseCase(projectId)
                uiState = uiState.copy(selectedProjectSlackChannels = channels)
            } catch (e: Exception) {
                uiState = uiState.copy(selectedProjectSlackChannels = emptyList())
            }
        }
    }

    private fun loadPreRunScripts(projectId: java.util.UUID) {
        scope.launch {
            try {
                val scripts = preRunScriptRepository.findByProject(projectId)
                uiState = uiState.copy(selectedProjectPreRunScripts = scripts)
            } catch (e: Exception) {
                uiState = uiState.copy(selectedProjectPreRunScripts = emptyList())
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
    fun unarchiveProject(projectId: java.util.UUID) {
        scope.launch {
            val result = unarchiveProjectUseCase(projectId)
            result.fold(
                onSuccess = {
                    loadProjects()
                    if (uiState.selectedProject?.id == projectId) {
                        uiState = uiState.copy(selectedProject = it)
                    }
                },
                onFailure = { error ->
                    uiState = uiState.copy(
                        error = error.message ?: "Failed to restore project"
                    )
                }
            )
        }
    }
    fun setShowArchivedProjects(show: Boolean) {
        uiState = uiState.copy(showArchivedProjects = show)
        loadProjects()
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

    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun setTagFilter(tag: String?) {
        uiState = uiState.copy(selectedTagFilter = tag)
    }

    fun setTeamFilter(team: String?) {
        uiState = uiState.copy(selectedTeamFilter = team)
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

    fun setDetailTab(tab: com.aitask.desktop.ui.projects.ProjectDetailTab) {
        uiState = uiState.copy(selectedDetailTab = tab)
    }

    fun showAddPreRunScriptDialog() {
        uiState = uiState.copy(showPreRunScriptDialog = true, editingPreRunScript = null, error = null)
    }

    fun showEditPreRunScriptDialog(script: PreRunScript) {
        uiState = uiState.copy(showPreRunScriptDialog = true, editingPreRunScript = script, error = null)
    }

    fun hidePreRunScriptDialog() {
        uiState = uiState.copy(showPreRunScriptDialog = false, editingPreRunScript = null, error = null)
    }

    fun savePreRunScript(
        name: String,
        type: PreRunScriptType,
        repositoryId: java.util.UUID?,
        executionOrder: Int,
        scriptPath: String?,
        inlineScript: String?,
        requiredValue: String?
    ) {
        val projectId = uiState.selectedProject?.id ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            uiState = uiState.copy(error = "Script name is required")
            return
        }

        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            try {
                val existing = uiState.editingPreRunScript
                val script = if (existing == null) {
                    PreRunScript(
                        id = java.util.UUID.randomUUID(),
                        projectId = projectId,
                        repositoryId = repositoryId,
                        name = trimmedName,
                        type = type,
                        scriptPath = scriptPath?.trim()?.ifBlank { null },
                        inlineScript = inlineScript?.trim()?.ifBlank { null },
                        requiredValue = requiredValue?.trim()?.ifBlank { null },
                        executionOrder = executionOrder,
                        createdAt = java.time.Instant.now(),
                        updatedAt = java.time.Instant.now()
                    )
                } else {
                    existing.update(
                        name = trimmedName,
                        type = type,
                        repositoryId = repositoryId,
                        executionOrder = executionOrder,
                        scriptPath = scriptPath?.trim()?.ifBlank { null },
                        inlineScript = inlineScript?.trim()?.ifBlank { null },
                        requiredValue = requiredValue?.trim()?.ifBlank { null }
                    )
                }

                validatePreRunScript(script)

                if (existing == null) {
                    preRunScriptRepository.create(script)
                } else {
                    preRunScriptRepository.update(script)
                }

                uiState = uiState.copy(
                    isSaving = false,
                    showPreRunScriptDialog = false,
                    editingPreRunScript = null
                )
                loadPreRunScripts(projectId)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save pre-run script"
                )
            }
        }
    }

    fun deletePreRunScript(scriptId: java.util.UUID) {
        val projectId = uiState.selectedProject?.id ?: return
        scope.launch {
            try {
                preRunScriptRepository.delete(scriptId)
                loadPreRunScripts(projectId)
            } catch (e: Exception) {
                uiState = uiState.copy(error = e.message ?: "Failed to delete pre-run script")
            }
        }
    }

    private fun validatePreRunScript(script: PreRunScript) {
        when (script.type) {
            PreRunScriptType.INLINE_COMMAND ->
                require(!script.inlineScript.isNullOrBlank()) { "Inline command is required" }

            PreRunScriptType.SCRIPT_PATH ->
                require(!script.scriptPath.isNullOrBlank()) { "Script path is required" }

            PreRunScriptType.NODE_VERSION,
            PreRunScriptType.JAVA_VERSION,
            PreRunScriptType.PYTHON_VERSION,
            PreRunScriptType.ENVIRONMENT_VARIABLE ->
                require(!script.requiredValue.isNullOrBlank()) { "Required value is required" }
        }
    }

    fun showAddSlackChannelDialog() {
        uiState = uiState.copy(showSlackChannelDialog = true, editingSlackChannel = null, error = null)
    }

    fun showEditSlackChannelDialog(channel: SlackChannelConfig) {
        uiState = uiState.copy(showSlackChannelDialog = true, editingSlackChannel = channel, error = null)
    }

    fun hideSlackChannelDialog() {
        uiState = uiState.copy(showSlackChannelDialog = false, editingSlackChannel = null, error = null)
    }

    fun createSlackChannel(webhookUrl: String, channelDisplayName: String, enabledEvents: Set<TaskEvent>) {
        val projectId = uiState.selectedProject?.id ?: return
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = CreateSlackChannelRequest(
                webhookUrl = webhookUrl,
                channelDisplayName = channelDisplayName,
                projectId = projectId,
                enabledEvents = enabledEvents
            )
            val result = createSlackChannelUseCase(request)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(isSaving = false, showSlackChannelDialog = false)
                    loadSlackChannels(projectId)
                },
                onFailure = { error ->
                    uiState = uiState.copy(isSaving = false, error = error.message ?: "Failed to add Slack channel")
                }
            )
        }
    }

    fun updateSlackChannel(id: java.util.UUID, webhookUrl: String?, channelDisplayName: String?, enabledEvents: Set<TaskEvent>?) {
        uiState = uiState.copy(isSaving = true, error = null)
        scope.launch {
            val request = UpdateSlackChannelRequest(
                webhookUrl = webhookUrl,
                channelDisplayName = channelDisplayName,
                enabledEvents = enabledEvents
            )
            val result = updateSlackChannelUseCase(id, request)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(isSaving = false, showSlackChannelDialog = false, editingSlackChannel = null)
                    uiState.selectedProject?.let { loadSlackChannels(it.id) }
                },
                onFailure = { error ->
                    uiState = uiState.copy(isSaving = false, error = error.message ?: "Failed to update Slack channel")
                }
            )
        }
    }

    fun deleteSlackChannel(channel: SlackChannelConfig) {
        scope.launch {
            val result = deleteSlackChannelUseCase(channel.id)
            result.fold(
                onSuccess = {
                    uiState.selectedProject?.let { loadSlackChannels(it.id) }
                },
                onFailure = { error ->
                    uiState = uiState.copy(error = error.message ?: "Failed to delete Slack channel")
                }
            )
        }
    }

    fun sendSlackTestMessage(channel: SlackChannelConfig) {
        scope.launch {
            uiState = uiState.copy(isSendingSlackTest = true, error = null)
            val result = sendSlackTestMessageUseCase(channel.id)
            uiState = uiState.copy(isSendingSlackTest = false)
            result.fold(
                onSuccess = { },
                onFailure = { error ->
                    uiState = uiState.copy(error = error.message ?: "Test message failed")
                }
            )
        }
    }
}

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val showArchivedProjects: Boolean = false,
    val selectedProject: Project? = null,
    val selectedProjectRepositories: List<Repository> = emptyList(),
    val selectedProjectPreRunScripts: List<PreRunScript> = emptyList(),
    val selectedProjectSlackChannels: List<SlackChannelConfig> = emptyList(),
    val selectedDetailTab: com.aitask.desktop.ui.projects.ProjectDetailTab = com.aitask.desktop.ui.projects.ProjectDetailTab.REPOSITORIES,
    val searchQuery: String = "",
    val selectedTagFilter: String? = null,
    val selectedTeamFilter: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showAddRepositoryDialog: Boolean = false,
    val showPreRunScriptDialog: Boolean = false,
    val showSlackChannelDialog: Boolean = false,
    val editingRepository: Repository? = null,
    val editingPreRunScript: PreRunScript? = null,
    val editingSlackChannel: SlackChannelConfig? = null,
    val isSendingSlackTest: Boolean = false,
    val error: String? = null
)
