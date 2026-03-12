package com.aitask.desktop.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.TaskType
import java.util.UUID

@Composable
fun CreateTaskDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String?,
        taskType: TaskType,
        projectId: UUID
    ) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.FEATURE) }
    var selectedProject by remember { mutableStateOf<Project?>(projects.firstOrNull()) }
    
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
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

                Divider()

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
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
                                    project.id
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
}

