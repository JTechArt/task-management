package com.aitask.desktop.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.model.TaskContentGenerationMode
import com.aitask.desktop.ui.viewmodel.TasksViewModel
import java.util.UUID

@Composable
fun CreateTaskDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    viewModel: TasksViewModel,
    onConfirm: (
        title: String,
        description: String?,
        taskType: TaskType,
        projectId: UUID,
        methodologyOverride: Methodology?
    ) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.FEATURE) }
    var methodologyOverride by remember { mutableStateOf<Methodology?>(null) }
    var selectedProject by remember { mutableStateOf<Project?>(projects.firstOrNull()) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState
    val hasUnsavedChanges = title.isNotBlank() || description.isNotBlank() ||
        selectedProject != projects.firstOrNull() || taskType != TaskType.FEATURE
    val requestDismiss: () -> Unit = {
        if (hasUnsavedChanges) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = { if (!isSaving) requestDismiss() }
    ) {
        Card(
            modifier = modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create New Task",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Divider()
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    minLines = 3,
                    maxLines = 5
                )

                if (uiState.taskContentSuggestion != null && uiState.taskContentGenerationTargetTaskId == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Generated suggestion",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.taskContentSuggestion!!,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    description = uiState.taskContentSuggestion!!
                                    viewModel.clearTaskContentSuggestion()
                                }) {
                                    Text("Use suggestion")
                                }
                                OutlinedButton(onClick = { viewModel.clearTaskContentSuggestion() }) {
                                    Text("Discard")
                                }
                            }
                        }
                    }
                }

                if (uiState.taskContentGenerationError != null && uiState.taskContentGenerationTargetTaskId == null) {
                    Text(
                        text = uiState.taskContentGenerationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            selectedProject?.let { project ->
                                viewModel.generateTaskContentSuggestion(
                                    projectId = project.id,
                                    title = title.ifBlank { "Task description" },
                                    currentDescription = description.takeIf { it.isNotBlank() },
                                    taskType = taskType,
                                    mode = TaskContentGenerationMode.DESCRIPTION
                                )
                            }
                        },
                        enabled = !isSaving && selectedProject != null && !uiState.isGeneratingTaskContent
                    ) {
                        Text(if (uiState.isGeneratingTaskContent) "Generating..." else "Generate description")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedProject?.let { project ->
                                viewModel.generateTaskContentSuggestion(
                                    projectId = project.id,
                                    title = title.ifBlank { "Task summary" },
                                    currentDescription = description.takeIf { it.isNotBlank() },
                                    taskType = taskType,
                                    mode = TaskContentGenerationMode.SUMMARY
                                )
                            }
                        },
                        enabled = !isSaving && selectedProject != null && !uiState.isGeneratingTaskContent
                    ) {
                        Text("Generate summary")
                    }
                }
                
                // Project dropdown
                var projectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = projectExpanded,
                    onExpandedChange = { if (!isSaving) projectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Project *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = projectExpanded,
                        onDismissRequest = { projectExpanded = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    selectedProject = project
                                    projectExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Task type dropdown
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { if (!isSaving) typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = taskType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Task Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        TaskType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    taskType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                var methodologyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodologyExpanded,
                    onExpandedChange = { if (!isSaving) methodologyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = methodologyOverride?.name ?: "Project Default",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Methodology Override") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodologyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving,
                        supportingText = { Text("Leave unset to inherit the project methodology") }
                    )
                    ExposedDropdownMenu(
                        expanded = methodologyExpanded,
                        onDismissRequest = { methodologyExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Project Default") },
                            onClick = {
                                methodologyOverride = null
                                methodologyExpanded = false
                            }
                        )
                        Methodology.values().forEach { methodology ->
                            DropdownMenuItem(
                                text = { Text(methodology.name) },
                                onClick = {
                                    methodologyOverride = methodology
                                    methodologyExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = requestDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            selectedProject?.let { project ->
                                onConfirm(
                                    title.trim(),
                                    description.trim().takeIf { it.isNotEmpty() },
                                    taskType,
                                    project.id,
                                    methodologyOverride
                                )
                            }
                        },
                        enabled = !isSaving &&
                                title.isNotBlank() &&
                                selectedProject != null
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSaving) "Creating..." else "Create Task")
                    }
                }
            }
        }
    }
    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Discard changes?") },
            text = { Text("Your changes will be lost. Are you sure you want to close?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}
