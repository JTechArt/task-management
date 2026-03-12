package com.aitask.desktop.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
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
            
            Button(
                onClick = { viewModel.showCreateDialog() },
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Project")
            }
        }
        
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
        } else if (uiState.projects.isEmpty()) {
            // Empty state
            EmptyProjectsState(
                onCreateClick = { viewModel.showCreateDialog() }
            )
        } else {
            // Projects list
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Projects list (left side)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.projects) { project ->
                            ProjectListItem(
                                project = project,
                                isSelected = project.id == uiState.selectedProject?.id,
                                onClick = { viewModel.selectProject(project) }
                            )
                        }
                    }
                }
                
                // Project detail (right side)
                if (uiState.selectedProject != null) {
                    Card(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        ProjectDetailView(
                            project = uiState.selectedProject,
                            repositories = uiState.selectedProjectRepositories,
                            onArchive = { viewModel.archiveProject(it) }
                        )
                    }
                }
            }
        }
    }
    
    // Create project dialog
    if (uiState.showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, description, workspacePath, branchTemplate, 
                          repoName, cloneUrl, provider, authType, ides ->
                viewModel.createProject(
                    name, description, workspacePath, branchTemplate,
                    repoName, cloneUrl, provider, authType, ides
                )
            },
            isSaving = uiState.isSaving
        )
    }
}

