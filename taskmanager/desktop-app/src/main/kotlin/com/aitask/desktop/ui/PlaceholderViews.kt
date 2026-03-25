package com.aitask.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.AgentTrigger
import com.aitask.desktop.ui.viewmodel.SettingsViewModel

@Composable
fun PlaceholderView(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF9E6)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = "This section is under development and will be available in a future release.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

// ProjectsView is now in projects/ProjectsView.kt
// TasksView is now in tasks/TasksView.kt
// RulesView is now in rules/RulesView.kt
// IntegrationsView is now in integrations/IntegrationsView.kt

@Composable
fun SettingsView(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = remember { SettingsViewModel() }
) {
    val uiState = viewModel.uiState
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure application settings, local LLM profiles, import/export data, and manage backups",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        LlmConfigurationCard(
            uiState = uiState,
            onNew = { viewModel.startNewLlmConfiguration() },
            onEdit = { viewModel.editLlmConfiguration(it) },
            onNameChange = { viewModel.updateLlmEditorName(it) },
            onEndpointChange = { viewModel.updateLlmEditorEndpoint(it) },
            onModelChange = { viewModel.updateLlmEditorModel(it) },
            onApiKeyChange = { viewModel.updateLlmEditorApiKey(it) },
            onDefaultChange = { viewModel.updateLlmEditorDefault(it) },
            onTest = { viewModel.testLlmConfigurationConnection() },
            onSave = { viewModel.saveLlmConfiguration() },
            onSetDefault = { viewModel.setDefaultLlmConfiguration(it) },
            onDelete = { viewModel.deleteLlmConfiguration(it) }
        )

        AgentDefinitionCard(
            uiState = uiState,
            onNew = { viewModel.startNewAgentDefinition() },
            onEdit = { viewModel.editAgentDefinition(it) },
            onNameChange = { viewModel.updateAgentEditorName(it) },
            onDescriptionChange = { viewModel.updateAgentEditorDescription(it) },
            onPromptChange = { viewModel.updateAgentEditorPromptTemplate(it) },
            onLlmConfigurationChange = { viewModel.updateAgentEditorLlmConfiguration(it) },
            onScopeChange = { viewModel.updateAgentEditorScope(it) },
            onProjectChange = { viewModel.updateAgentEditorProjectId(it) },
            onTriggerChange = { viewModel.updateAgentEditorTrigger(it) },
            onEnabledChange = { viewModel.updateAgentEditorEnabled(it) },
            onSave = { viewModel.saveAgentDefinition() },
            onDelete = { viewModel.deleteAgentDefinition(it) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import / Export",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Export your projects, tasks, repositories, and rules to a JSON file. Import data from a previously exported file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportData() },
                        enabled = !uiState.isLoading
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export")
                    }
                    Button(
                        onClick = { viewModel.importData() },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import")
                    }
                }
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create a backup of your data for local recovery. Restore from a previously created backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupData() },
                        enabled = !uiState.isLoading
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Run backup now")
                    }
                    Button(
                        onClick = { viewModel.initiateRestore() },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Restore from file")
                    }
                }
            }
        }

        if (uiState.showRestoreConfirmation && uiState.pendingRestoreSummary != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelRestore() },
                title = { Text("Confirm Restore") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Restoring will add the backup data alongside your current data. Projects and items with matching names or paths may be renamed to avoid conflicts.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = uiState.pendingRestoreSummary!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmRestore() }) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelRestore() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) { Text(error) }
        }

        uiState.llmError?.let { error ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.startNewLlmConfiguration() }) {
                        Text("Edit")
                    }
                }
            ) { Text(error) }
        }

        uiState.exportFeedback?.let { feedback ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearExportFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) { Text(feedback) }
        }

        uiState.importFeedback?.let { feedback ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearImportFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) { Text(feedback) }
        }

        uiState.restoreFeedback?.let { feedback ->
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearRestoreFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) { Text(feedback) }
        }
    }
}

@Composable
private fun LlmConfigurationCard(
    uiState: com.aitask.desktop.ui.viewmodel.SettingsUiState,
    onNew: () -> Unit,
    onEdit: (LlmConfiguration) -> Unit,
    onNameChange: (String) -> Unit,
    onEndpointChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDefaultChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onSetDefault: (java.util.UUID) -> Unit,
    onDelete: (java.util.UUID) -> Unit
) {
    val editor = uiState.llmEditor
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Local LLM profiles",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Store one or more local model endpoints and choose the default profile for AI-assisted features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onNew) {
                    Text("New profile")
                }
            }

            uiState.llmFeedback?.let { feedback ->
                Snackbar(modifier = Modifier.fillMaxWidth()) { Text(feedback) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editor.endpointUrl,
                    onValueChange = onEndpointChange,
                    label = { Text("Endpoint URL") },
                    placeholder = { Text("http://localhost:11434") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editor.modelIdentifier,
                    onValueChange = onModelChange,
                    label = { Text("Model identifier") },
                    placeholder = { Text("llama3.1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editor.apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API key or token") },
                    placeholder = { Text("Leave blank to keep the stored key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = editor.isDefault,
                        onCheckedChange = onDefaultChange
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Use as default",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "This profile will be selected automatically for AI-assisted features.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTest,
                        enabled = !uiState.isLlmSaving && !uiState.isLlmTesting
                    ) {
                        Text(if (uiState.isLlmTesting) "Testing…" else "Test connection")
                    }
                    Button(
                        onClick = onSave,
                        enabled = !uiState.isLlmSaving && !uiState.isLlmTesting
                    ) {
                        Text(if (editor.id == null) "Save profile" else "Update profile")
                    }
                    if (uiState.isLlmLoading) {
                        CircularProgressIndicator(modifier = Modifier.width(20.dp))
                    }
                }
            }

            if (uiState.llmConfigurations.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No LLM profiles saved yet. Add one above to enable local AI features.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.llmConfigurations.forEach { configuration ->
                        LlmConfigurationRow(
                            configuration = configuration,
                            onEdit = onEdit,
                            onSetDefault = onSetDefault,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentDefinitionCard(
    uiState: com.aitask.desktop.ui.viewmodel.SettingsUiState,
    onNew: () -> Unit,
    onEdit: (AgentDefinition) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onLlmConfigurationChange: (java.util.UUID?) -> Unit,
    onScopeChange: (AgentScope) -> Unit,
    onProjectChange: (java.util.UUID?) -> Unit,
    onTriggerChange: (AgentTrigger) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: (java.util.UUID) -> Unit
) {
    val editor = uiState.agentEditor
    var scopeExpanded by remember { mutableStateOf(false) }
    var triggerExpanded by remember { mutableStateOf(false) }
    var llmExpanded by remember { mutableStateOf(false) }
    var projectExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Agent builder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create reusable LLM-powered agents with prompt templates and project scope.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onNew) {
                    Text("New agent")
                }
            }

            uiState.agentFeedback?.let { feedback ->
                Snackbar(modifier = Modifier.fillMaxWidth()) { Text(feedback) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text("Agent name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editor.promptTemplate,
                    onValueChange = onPromptChange,
                    label = { Text("Prompt template") },
                    placeholder = { Text("Use {{taskTitle}}, {{projectName}}, {{branchName}}, {{repositoryNames}}") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = llmExpanded,
                        onExpandedChange = { llmExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.llmConfigurations.find { it.id == editor.llmConfigurationId }?.name ?: "Default profile",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("LLM profile") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = llmExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = llmExpanded, onDismissRequest = { llmExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Default profile") },
                                onClick = {
                                    onLlmConfigurationChange(null)
                                    llmExpanded = false
                                }
                            )
                            uiState.llmConfigurations.forEach { configuration ->
                                DropdownMenuItem(
                                    text = { Text(configuration.name) },
                                    onClick = {
                                        onLlmConfigurationChange(configuration.id)
                                        llmExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = scopeExpanded,
                        onExpandedChange = { scopeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = editor.scope.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Scope") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = scopeExpanded, onDismissRequest = { scopeExpanded = false }) {
                            AgentScope.entries.forEach { scope ->
                                DropdownMenuItem(
                                    text = { Text(scope.name) },
                                    onClick = {
                                        onScopeChange(scope)
                                        scopeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (editor.scope == AgentScope.PROJECT) {
                    ExposedDropdownMenuBox(
                        expanded = projectExpanded,
                        onExpandedChange = { projectExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.projects.find { it.id == editor.projectId }?.name ?: "Select project",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = projectExpanded, onDismissRequest = { projectExpanded = false }) {
                            uiState.projects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.name) },
                                    onClick = {
                                        onProjectChange(project.id)
                                        projectExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = triggerExpanded,
                        onExpandedChange = { triggerExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = editor.trigger.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Trigger") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = triggerExpanded, onDismissRequest = { triggerExpanded = false }) {
                            AgentTrigger.entries.forEach { trigger ->
                                DropdownMenuItem(
                                    text = { Text(trigger.name) },
                                    onClick = {
                                        onTriggerChange(trigger)
                                        triggerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(checked = editor.isEnabled, onCheckedChange = onEnabledChange)
                        Spacer(Modifier.width(8.dp))
                        Text("Enabled")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onSave,
                        enabled = !uiState.isAgentSaving
                    ) {
                        Text(if (editor.id == null) "Save agent" else "Update agent")
                    }
                    if (uiState.isAgentLoading) {
                        CircularProgressIndicator(modifier = Modifier.width(20.dp))
                    }
                }
            }

            if (uiState.agentDefinitions.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No agents saved yet. Add one above to automate task-driven workflows.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.agentDefinitions.forEach { definition ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
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
                                        Text(definition.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${definition.scope.name} • ${definition.trigger.name}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = { onEdit(definition) }) { Text("Edit") }
                                        TextButton(onClick = { onDelete(definition.id) }) { Text("Delete") }
                                    }
                                }
                                definition.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            uiState.agentError?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LlmConfigurationRow(
    configuration: LlmConfiguration,
    onEdit: (LlmConfiguration) -> Unit,
    onSetDefault: (java.util.UUID) -> Unit,
    onDelete: (java.util.UUID) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = configuration.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (configuration.isDefault) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "Default",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Text(
                        text = configuration.endpointUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Model: ${configuration.modelIdentifier} • ${if (configuration.hasApiKey) "API key stored" else "No API key"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onEdit(configuration) }) {
                    Text("Edit")
                }
                if (!configuration.isDefault) {
                    TextButton(onClick = { onSetDefault(configuration.id) }) {
                        Text("Set default")
                    }
                }
                TextButton(
                    onClick = { onDelete(configuration.id) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
