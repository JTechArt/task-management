package com.aitask.desktop.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        description: String?,
        workspacePath: String,
        branchTemplate: String,
        repositoryName: String,
        cloneUrl: String,
        provider: GitProvider,
        authType: AuthType,
        preferredIDEs: List<IDEType>
    ) -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var workspacePath by remember { mutableStateOf("") }
    var branchTemplate by remember { mutableStateOf("task-{taskId}") }
    var repositoryName by remember { mutableStateOf("") }
    var cloneUrl by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf(GitProvider.GITHUB) }
    var authType by remember { mutableStateOf(AuthType.HTTPS) }
    var selectedIDEs by remember { mutableStateOf(setOf<IDEType>()) }
    
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = modifier.width(600.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create New Project",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Divider()
                
                // Project section
                Text(
                    text = "Project Details",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name *") },
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
                    minLines = 2,
                    maxLines = 3
                )
                
                OutlinedTextField(
                    value = workspacePath,
                    onValueChange = { workspacePath = it },
                    label = { Text("Workspace Path *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    placeholder = { Text("/Users/username/workspaces") }
                )
                
                OutlinedTextField(
                    value = branchTemplate,
                    onValueChange = { branchTemplate = it },
                    label = { Text("Branch Template *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    supportingText = { Text("Use {taskId} as placeholder") }
                )
                
                Divider()
                
                // Repository section
                Text(
                    text = "Primary Repository",
                    style = MaterialTheme.typography.titleMedium
                )
                
                OutlinedTextField(
                    value = repositoryName,
                    onValueChange = { repositoryName = it },
                    label = { Text("Repository Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = cloneUrl,
                    onValueChange = { cloneUrl = it },
                    label = { Text("Clone URL *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    placeholder = { Text("https://github.com/user/repo.git") }
                )
                
                // Provider dropdown
                var providerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { if (!isSaving) providerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = provider.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Git Provider *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        GitProvider.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    provider = p
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Auth type dropdown
                var authExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = authExpanded,
                    onExpandedChange = { if (!isSaving) authExpanded = it }
                ) {
                    OutlinedTextField(
                        value = authType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Auth Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = authExpanded,
                        onDismissRequest = { authExpanded = false }
                    ) {
                        AuthType.values().forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.name) },
                                onClick = {
                                    authType = a
                                    authExpanded = false
                                }
                            )
                        }
                    }
                }

                // IDE selection
                Text(
                    text = "Preferred IDEs *",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IDEType.values().forEach { ide ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedIDEs.contains(ide),
                                onCheckedChange = { checked ->
                                    selectedIDEs = if (checked) {
                                        selectedIDEs + ide
                                    } else {
                                        selectedIDEs - ide
                                    }
                                },
                                enabled = !isSaving
                            )
                            Text(
                                text = ide.name,
                                style = MaterialTheme.typography.bodyMedium
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
                            onConfirm(
                                name.trim(),
                                description.trim().takeIf { it.isNotEmpty() },
                                workspacePath.trim(),
                                branchTemplate.trim(),
                                repositoryName.trim(),
                                cloneUrl.trim(),
                                provider,
                                authType,
                                selectedIDEs.toList()
                            )
                        },
                        enabled = !isSaving &&
                                name.isNotBlank() &&
                                workspacePath.isNotBlank() &&
                                branchTemplate.isNotBlank() &&
                                repositoryName.isNotBlank() &&
                                cloneUrl.isNotBlank() &&
                                selectedIDEs.isNotEmpty()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (isSaving) "Creating..." else "Create Project")
                    }
                }
            }
        }
    }
}

