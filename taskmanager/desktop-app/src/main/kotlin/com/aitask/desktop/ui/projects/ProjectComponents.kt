package com.aitask.desktop.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.aitask.core.domain.model.EnvironmentCheckTemplateId
import com.aitask.core.domain.model.EnvironmentCheckTemplateRegistry
import com.aitask.core.domain.model.BmadTool
import com.aitask.core.domain.model.BmadToolCatalog
import com.aitask.core.domain.model.BmadToolType
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.PreRunScript
import com.aitask.core.domain.model.PreRunScriptType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.SlackChannelConfig
import java.util.UUID

enum class ProjectDetailTab { REPOSITORIES, PRE_RUN, SLACK }

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
    onShowArchivedClick: (() -> Unit)? = null,
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
            onShowArchivedClick?.let { onShow ->
                TextButton(
                    onClick = onShow,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Show archived projects")
                }
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
    onUnarchive: ((UUID) -> Unit)? = null,
    onSaveMethodology: (UUID, Methodology) -> Unit = { _, _ -> },
    onSaveBmadTools: (UUID, List<String>) -> Unit = { _, _ -> },
    hasAttachedRules: Boolean = false,
    onAddRepository: () -> Unit = {},
    onEditRepository: (Repository) -> Unit = {},
    onDeleteRepository: (UUID) -> Unit = {},
    preRunScripts: List<PreRunScript> = emptyList(),
    slackChannels: List<SlackChannelConfig> = emptyList(),
    selectedDetailTab: ProjectDetailTab = ProjectDetailTab.REPOSITORIES,
    onDetailTabChange: (ProjectDetailTab) -> Unit = {},
    onAddPreRunScript: () -> Unit = {},
    onEditPreRunScript: (PreRunScript) -> Unit = {},
    onDeletePreRunScript: (UUID) -> Unit = {},
    onAddSlackChannel: () -> Unit = {},
    onEditSlackChannel: (SlackChannelConfig) -> Unit = {},
    onDeleteSlackChannel: (SlackChannelConfig) -> Unit = {},
    onTestSlackMessage: (SlackChannelConfig) -> Unit = {},
    isSendingSlackTest: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(selectedDetailTab) { mutableStateOf(selectedDetailTab) }
    var selectedMethodology by remember(project.methodology) { mutableStateOf(project.methodology) }
    var selectedBmadToolIds by remember(project.bmadToolIds) {
        mutableStateOf(project.bmadToolIds.ifEmpty { BmadToolCatalog.defaultToolIds })
    }
    var methodologyExpanded by remember { mutableStateOf(false) }
    var showMethodologyConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(selectedDetailTab) { selectedTab = selectedDetailTab }
    LaunchedEffect(project.methodology) { selectedMethodology = project.methodology }
    LaunchedEffect(project.bmadToolIds) {
        selectedBmadToolIds = project.bmadToolIds.ifEmpty { BmadToolCatalog.defaultToolIds }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            if (project.isArchived && onUnarchive != null) {
                Button(
                    onClick = { onUnarchive(project.id) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Unarchive, contentDescription = "Restore project", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Restore")
                }
            } else if (!project.isArchived) {
                IconButton(onClick = { onArchive(project.id) }) {
                    Icon(Icons.Default.Archive, contentDescription = "Archive project")
                }
            }
        }
        project.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        DetailRow("Workspace Path", project.workspacePath)
        DetailRow("Branch Template", project.branchTemplate)
        DetailRow("Methodology", project.methodology.name)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ExposedDropdownMenuBox(
                expanded = methodologyExpanded,
                onExpandedChange = { methodologyExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedMethodology.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Project Methodology") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodologyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    supportingText = { Text("Tasks inherit this unless they override it") }
                )
                ExposedDropdownMenu(
                    expanded = methodologyExpanded,
                    onDismissRequest = { methodologyExpanded = false }
                ) {
                    Methodology.values().forEach { methodology ->
                        DropdownMenuItem(
                            text = { Text(methodology.name) },
                            onClick = {
                                selectedMethodology = methodology
                                methodologyExpanded = false
                            }
                        )
                    }
                }
            }
            Button(
                onClick = {
                    if (hasAttachedRules && selectedMethodology != project.methodology) {
                        showMethodologyConfirmation = true
                    } else {
                        onSaveMethodology(project.id, selectedMethodology)
                    }
                },
                enabled = selectedMethodology != project.methodology
            ) {
                Text("Save")
            }
        }
        if (project.tags.isNotEmpty()) {
            DetailRow("Tags", project.tags.joinToString(", "))
        }
        if (project.methodology == Methodology.BMAD) {
            Divider()
            Text(
                text = "BMAD Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These tools are surfaced in BMAD task context and can be customized per task.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            BmadToolSelectionSection(
                selectedToolIds = selectedBmadToolIds,
                onSelectionChange = { selectedBmadToolIds = it }
            )
            Button(
                onClick = { onSaveBmadTools(project.id, selectedBmadToolIds) },
                enabled = selectedBmadToolIds != project.bmadToolIds
            ) {
                Text("Save BMAD Tools")
            }
        }
        project.team?.let { team ->
            DetailRow("Team", team)
        }
        Divider()
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == ProjectDetailTab.REPOSITORIES,
                onClick = {
                    selectedTab = ProjectDetailTab.REPOSITORIES
                    onDetailTabChange(ProjectDetailTab.REPOSITORIES)
                },
                text = { Text("Repositories") }
            )
            Tab(
                selected = selectedTab == ProjectDetailTab.PRE_RUN,
                onClick = {
                    selectedTab = ProjectDetailTab.PRE_RUN
                    onDetailTabChange(ProjectDetailTab.PRE_RUN)
                },
                text = { Text("Pre-Run") }
            )
            Tab(
                selected = selectedTab == ProjectDetailTab.SLACK,
                onClick = {
                    selectedTab = ProjectDetailTab.SLACK
                    onDetailTabChange(ProjectDetailTab.SLACK)
                },
                text = { Text("Slack") }
            )
        }
        when (selectedTab) {
            ProjectDetailTab.REPOSITORIES -> {
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
            ProjectDetailTab.PRE_RUN -> {
                val projectLevelScripts = preRunScripts.filter { it.repositoryId == null }
                val repositoryScripts = preRunScripts.filter { it.repositoryId != null }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Pre-Run Scripts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Run checks before opening the IDE",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Execution order: project scripts first, then repository scripts in workspace repository order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = onAddPreRunScript,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Check", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (preRunScripts.isEmpty()) {
                    Text(
                        text = "No pre-run scripts configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PreRunScriptGroup(
                            title = "Project-Level Scripts",
                            emptyText = "No project-level scripts configured",
                            scripts = projectLevelScripts,
                            repositories = repositories,
                            onEditPreRunScript = onEditPreRunScript,
                            onDeletePreRunScript = onDeletePreRunScript
                        )
                        PreRunScriptGroup(
                            title = "Repository-Level Scripts",
                            emptyText = "No repository-level scripts configured",
                            scripts = repositoryScripts,
                            repositories = repositories,
                            onEditPreRunScript = onEditPreRunScript,
                            onDeletePreRunScript = onDeletePreRunScript
                        )
                    }
                }
            }
            ProjectDetailTab.SLACK -> {
                SlackConfigView(
                    channels = slackChannels,
                    onAddChannel = onAddSlackChannel,
                    onEditChannel = onEditSlackChannel,
                    onDeleteChannel = onDeleteSlackChannel,
                    onTestMessage = onTestSlackMessage,
                    isSendingTest = isSendingSlackTest
                )
            }
        }
            }
        }
    }
    if (showMethodologyConfirmation) {
        AlertDialog(
            onDismissRequest = { showMethodologyConfirmation = false },
            title = { Text("Change methodology?") },
            text = {
                Text("This project already has attached rule sets. Changing methodology will not delete them, but BMAD-specific behavior may change.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMethodologyConfirmation = false
                        onSaveMethodology(project.id, selectedMethodology)
                    }
                ) {
                    Text("Change Methodology")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMethodologyConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BmadToolSelectionSection(
    selectedToolIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BmadToolType.values().forEach { type ->
            val tools = BmadToolCatalog.tools.filter { it.type == type }
            if (tools.isNotEmpty()) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                tools.forEach { tool ->
                    BmadToolCheckboxRow(
                        tool = tool,
                        selected = selectedToolIds.contains(tool.id),
                        onSelectedChange = { checked ->
                            onSelectionChange(
                                if (checked) {
                                    (selectedToolIds + tool.id).distinct()
                                } else {
                                    selectedToolIds - tool.id
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BmadToolCheckboxRow(
    tool: BmadTool,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(tool.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PreRunScriptGroup(
    title: String,
    emptyText: String,
    scripts: List<PreRunScript>,
    repositories: List<Repository>,
    onEditPreRunScript: (PreRunScript) -> Unit,
    onDeletePreRunScript: (UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (scripts.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                scripts.forEach { script ->
                    PreRunScriptCard(
                        script = script,
                        repositories = repositories,
                        onEdit = { onEditPreRunScript(script) },
                        onDelete = { onDeletePreRunScript(script.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PreRunScriptCard(
    script: PreRunScript,
    repositories: List<Repository>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scopeLabel = repositories.find { it.id == script.repositoryId }?.name ?: "Project"
    val template = EnvironmentCheckTemplateRegistry.findByType(script.type)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = template?.name ?: script.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit pre-run script", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete pre-run script",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            DetailRow("Scope", scopeLabel)
            DetailRow("Order", script.executionOrder.toString())
            when (script.type) {
                PreRunScriptType.INLINE_COMMAND -> DetailRow("Command", script.inlineScript ?: "")
                PreRunScriptType.SCRIPT_PATH -> DetailRow("Path", script.scriptPath ?: "")
                else -> DetailRow("Requirement", script.requiredValue ?: "")
            }
        }
    }
}

@Composable
fun PreRunScriptDialog(
    script: PreRunScript?,
    repositories: List<Repository>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: PreRunScriptType,
        repositoryId: UUID?,
        executionOrder: Int,
        scriptPath: String?,
        inlineScript: String?,
        requiredValue: String?
    ) -> Unit,
    isSaving: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    var name by remember(script) { mutableStateOf(script?.name ?: "") }
    var type by remember(script) { mutableStateOf(script?.type ?: PreRunScriptType.INLINE_COMMAND) }
    var repositoryId by remember(script) { mutableStateOf(script?.repositoryId) }
    var executionOrder by remember(script) { mutableStateOf((script?.executionOrder ?: 0).toString()) }
    var scriptPath by remember(script) { mutableStateOf(script?.scriptPath ?: "") }
    var inlineScript by remember(script) { mutableStateOf(script?.inlineScript ?: "") }
    var requiredValue by remember(script) { mutableStateOf(script?.requiredValue ?: "") }
    var typeExpanded by remember { mutableStateOf(false) }
    var scopeExpanded by remember { mutableStateOf(false) }
    val templates = remember { EnvironmentCheckTemplateRegistry.all() }
    val selectedTemplate = remember(type) { EnvironmentCheckTemplateRegistry.findByType(type) }
    var templateExpanded by remember { mutableStateOf(false) }
    val previewScript = remember(type, requiredValue, inlineScript, scriptPath) {
        when {
            selectedTemplate != null && requiredValue.isNotBlank() ->
                selectedTemplate.generatePreview(requiredValue.trim(), isWindows = false)
            type == PreRunScriptType.INLINE_COMMAND -> inlineScript
            type == PreRunScriptType.SCRIPT_PATH -> scriptPath
            else -> ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (script == null) "Add Pre-Run Script" else "Edit Pre-Run Script") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                error?.let {
                    AssistChip(
                        onClick = onDismissError,
                        label = { Text(it) }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = templateExpanded,
                    onExpandedChange = { templateExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTemplate?.name ?: "Custom Script",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Template") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = templateExpanded,
                        onDismissRequest = { templateExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Custom Script") },
                            onClick = {
                                templateExpanded = false
                            }
                        )
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(template.name)
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                },
                                onClick = {
                                    type = template.scriptType
                                    if (requiredValue.isBlank()) {
                                        requiredValue = template.exampleValue
                                    }
                                    if (script == null || name.isBlank() || EnvironmentCheckTemplateRegistry.findByType(script.type) != null) {
                                        name = template.generateName(requiredValue.ifBlank { template.exampleValue })
                                    }
                                    templateExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type.name.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        PreRunScriptType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.replace('_', ' ')) },
                                onClick = {
                                    type = option
                                    if (EnvironmentCheckTemplateRegistry.findByType(option) == null) {
                                        requiredValue = ""
                                    }
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = scopeExpanded,
                    onExpandedChange = { scopeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = repositories.find { it.id == repositoryId }?.name ?: "Project",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scope") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = scopeExpanded,
                        onDismissRequest = { scopeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Project") },
                            onClick = {
                                repositoryId = null
                                scopeExpanded = false
                            }
                        )
                        repositories.forEach { repository ->
                            DropdownMenuItem(
                                text = { Text(repository.name) },
                                onClick = {
                                    repositoryId = repository.id
                                    scopeExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = executionOrder,
                    onValueChange = { executionOrder = it.filter(Char::isDigit) },
                    label = { Text("Execution Order") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                when (type) {
                    PreRunScriptType.INLINE_COMMAND -> {
                        OutlinedTextField(
                            value = inlineScript,
                            onValueChange = { inlineScript = it },
                            label = { Text("Command") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                    PreRunScriptType.SCRIPT_PATH -> {
                        OutlinedTextField(
                            value = scriptPath,
                            onValueChange = { scriptPath = it },
                            label = { Text("Script Path") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    PreRunScriptType.NODE_VERSION,
                    PreRunScriptType.JAVA_VERSION,
                    PreRunScriptType.PYTHON_VERSION -> {
                        OutlinedTextField(
                            value = requiredValue,
                            onValueChange = { requiredValue = it },
                            label = { Text(selectedTemplate?.parameterLabel ?: "Required Version") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    PreRunScriptType.ENVIRONMENT_VARIABLE,
                    PreRunScriptType.DEPENDENCY_PRESENT -> {
                        OutlinedTextField(
                            value = requiredValue,
                            onValueChange = { requiredValue = it },
                            label = { Text(selectedTemplate?.parameterLabel ?: "Required Value") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                if (selectedTemplate != null) {
                    Text(
                        text = selectedTemplate.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (previewScript.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Generated Script Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = previewScript,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        type,
                        repositoryId,
                        executionOrder.toIntOrNull() ?: 0,
                        scriptPath,
                        inlineScript,
                        requiredValue
                    )
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
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
