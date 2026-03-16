package com.aitask.desktop.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import java.util.UUID

@Composable
fun ProjectFilters(
    availableTags: List<String>,
    availableTeams: List<String>,
    selectedTag: String?,
    selectedTeam: String?,
    searchQuery: String,
    onTagSelected: (String?) -> Unit,
    onTeamSelected: (String?) -> Unit,
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
            label = { Text("Search projects") },
            placeholder = { Text("Search by name or description...") },
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
            if (availableTags.isNotEmpty()) {
                var tagExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = tagExpanded,
                    onExpandedChange = { tagExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedTag ?: "All Tags",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tag") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = tagExpanded,
                        onDismissRequest = { tagExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Tags") },
                            onClick = {
                                onTagSelected(null)
                                tagExpanded = false
                            }
                        )
                        availableTags.sorted().forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    onTagSelected(tag)
                                    tagExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            if (availableTeams.isNotEmpty()) {
                var teamExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = teamExpanded,
                    onExpandedChange = { teamExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedTeam ?: "All Teams",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Team") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teamExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = teamExpanded,
                        onDismissRequest = { teamExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Teams") },
                            onClick = {
                                onTeamSelected(null)
                                teamExpanded = false
                            }
                        )
                        availableTeams.sorted().forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team) },
                                onClick = {
                                    onTeamSelected(team)
                                    teamExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyProjectsState(
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
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Create your first project to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Project")
            }
        }
    }
}

@Composable
fun ProjectListItem(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit,
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            project.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 2
                )
            }
            Text(
                text = project.workspacePath,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProjectDetailView(
    project: Project,
    repositories: List<Repository>,
    onArchive: (UUID) -> Unit,
    onAddRepository: () -> Unit = {},
    onEditRepository: (Repository) -> Unit = {},
    onDeleteRepository: (UUID) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with archive button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onArchive(project.id) }) {
                Icon(Icons.Default.Archive, contentDescription = "Archive project")
            }
        }
        
        // Description
        project.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Divider()
        
        // Project details
        DetailRow("Workspace Path", project.workspacePath)
        DetailRow("Branch Template", project.branchTemplate)
        
        if (project.tags.isNotEmpty()) {
            DetailRow("Tags", project.tags.joinToString(", "))
        }

        project.team?.let { team ->
            DetailRow("Team", team)
        }

        Divider()

        // Repositories section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Repositories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddRepository,
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Repository", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (repositories.isEmpty()) {
            Text(
                text = "No repositories configured",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            repositories.forEach { repo ->
                RepositoryCard(
                    repository = repo,
                    onEdit = { onEditRepository(repo) },
                    onDelete = { onDeleteRepository(repo.id) }
                )
            }
        }
    }
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
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun RepositoryCard(
    repository: Repository,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = repository.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (repository.isPrimary) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PRIMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit repository",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!repository.isPrimary) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete repository",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Text(
                text = repository.cloneUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Provider:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = repository.provider.name,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Auth:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = repository.authType.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (repository.preferredIDEs.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IDEs:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    repository.preferredIDEs.forEach { ide ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = ide.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

