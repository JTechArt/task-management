package com.aitask.desktop.ui.projects

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
import com.aitask.core.domain.model.Project
import com.aitask.desktop.ui.viewmodel.ProjectsViewModel

@Composable
fun ProjectsView(
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = remember { ProjectsViewModel() }
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
                    text = "Projects",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage multi-repository projects and workspace configurations",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.showArchivedProjects) {
                    OutlinedButton(
                        onClick = { viewModel.setShowArchivedProjects(false) },
                        enabled = !uiState.isLoading
                    ) {
                        Text("Hide archived")
                    }
                }
                Button(
                    onClick = { viewModel.showCreateDialog() },
                    enabled = !uiState.isLoading
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Project")
                }
            }
        }
        
        // Error message (only show when dialog is not open)
        if (!uiState.showCreateDialog) {
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
        }
        
        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.projects.isEmpty()) {
            // Empty state - show option to view archived when not already showing archived
            EmptyProjectsState(
                onCreateClick = { viewModel.showCreateDialog() },
                onShowArchivedClick = if (uiState.showArchivedProjects) null else { { viewModel.setShowArchivedProjects(true) } }
            )
        } else {
            val availableTags = remember(uiState.projects) {
                uiState.projects.flatMap { it.tags }.distinct()
            }
            val availableTeams = remember(uiState.projects) {
                uiState.projects.mapNotNull { it.team }.distinct()
            }
            val filteredProjects = remember(
                uiState.projects,
                uiState.searchQuery,
                uiState.selectedTagFilter,
                uiState.selectedTeamFilter
            ) {
                uiState.projects.filter { project ->
                    val query = uiState.searchQuery.trim().lowercase()
                    val searchMatch = query.isEmpty() ||
                        project.name.lowercase().contains(query) ||
                        (project.description?.lowercase()?.contains(query) == true)
                    val tagMatch = uiState.selectedTagFilter == null ||
                        project.tags.contains(uiState.selectedTagFilter)
                    val teamMatch = uiState.selectedTeamFilter == null ||
                        project.team == uiState.selectedTeamFilter
                    searchMatch && tagMatch && teamMatch
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProjectFilters(
                    availableTags = availableTags,
                    availableTeams = availableTeams,
                    selectedTag = uiState.selectedTagFilter,
                    selectedTeam = uiState.selectedTeamFilter,
                    searchQuery = uiState.searchQuery,
                    onTagSelected = { viewModel.setTagFilter(it) },
                    onTeamSelected = { viewModel.setTeamFilter(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) }
                )
                if (filteredProjects.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No projects match the current filters",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredProjects) { project ->
                                    ProjectListItem(
                                        project = project,
                                        isSelected = project.id == uiState.selectedProject?.id,
                                        onClick = { viewModel.selectProject(project) }
                                    )
                                }
                            }
                        }
                        if (uiState.selectedProject != null) {
                            Card(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .fillMaxHeight()
                            ) {
                                ProjectDetailView(
                                    project = uiState.selectedProject,
                                    repositories = uiState.selectedProjectRepositories,
                                    onArchive = { viewModel.archiveProject(it) },
                                    onUnarchive = { viewModel.unarchiveProject(it) },
                                    onAddRepository = { viewModel.showAddRepositoryDialog() },
                                    onEditRepository = { viewModel.showEditRepositoryDialog(it) },
                                    onDeleteRepository = { viewModel.deleteRepository(it) },
                                    preRunScripts = uiState.selectedProjectPreRunScripts,
                                    slackChannels = uiState.selectedProjectSlackChannels,
                                    selectedDetailTab = uiState.selectedDetailTab,
                                    onDetailTabChange = { viewModel.setDetailTab(it) },
                                    onAddPreRunScript = { viewModel.showAddPreRunScriptDialog() },
                                    onEditPreRunScript = { viewModel.showEditPreRunScriptDialog(it) },
                                    onDeletePreRunScript = { viewModel.deletePreRunScript(it) },
                                    onAddSlackChannel = { viewModel.showAddSlackChannelDialog() },
                                    onEditSlackChannel = { viewModel.showEditSlackChannelDialog(it) },
                                    onDeleteSlackChannel = { viewModel.deleteSlackChannel(it) },
                                    onTestSlackMessage = { viewModel.sendSlackTestMessage(it) },
                                    isSendingSlackTest = uiState.isSendingSlackTest
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showPreRunScriptDialog && uiState.selectedProject != null) {
        PreRunScriptDialog(
            script = uiState.editingPreRunScript,
            repositories = uiState.selectedProjectRepositories,
            onDismiss = { viewModel.hidePreRunScriptDialog() },
            onConfirm = { name, type, repositoryId, executionOrder, scriptPath, inlineScript, requiredValue ->
                viewModel.savePreRunScript(
                    name = name,
                    type = type,
                    repositoryId = repositoryId,
                    executionOrder = executionOrder,
                    scriptPath = scriptPath,
                    inlineScript = inlineScript,
                    requiredValue = requiredValue
                )
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }
    
    // Create project dialog
    if (uiState.showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, description, workspacePath, branchTemplate, retentionPolicy,
                          repoName, cloneUrl, provider, authType, ides ->
                viewModel.createProject(
                    name, description, workspacePath, branchTemplate, retentionPolicy,
                    repoName, cloneUrl, provider, authType, ides
                )
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }

    // Add/Edit repository dialog
    if (uiState.showAddRepositoryDialog && uiState.selectedProject != null) {
        RepositoryDialog(
            repository = uiState.editingRepository,
            projectId = uiState.selectedProject.id,
            onDismiss = { viewModel.hideRepositoryDialog() },
            onConfirm = { name, cloneUrl, provider, authType, ides, isPrimary ->
                if (uiState.editingRepository != null) {
                    // Edit existing repository
                    viewModel.updateRepository(
                        repositoryId = uiState.editingRepository.id,
                        name = name,
                        cloneUrl = cloneUrl,
                        provider = provider,
                        authType = authType,
                        preferredIDEs = ides,
                        isPrimary = isPrimary
                    )
                } else {
                    // Create new repository
                    viewModel.createRepository(
                        projectId = uiState.selectedProject.id,
                        name = name,
                        cloneUrl = cloneUrl,
                        provider = provider,
                        authType = authType,
                        preferredIDEs = ides,
                        isPrimary = isPrimary
                    )
                }
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }
}
