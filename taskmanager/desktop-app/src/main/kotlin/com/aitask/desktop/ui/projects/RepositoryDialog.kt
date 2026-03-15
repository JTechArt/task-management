package com.aitask.desktop.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.*

@Composable
fun RepositoryDialog(
    repository: Repository? = null, // null for create, non-null for edit
    projectId: java.util.UUID,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        cloneUrl: String,
        provider: GitProvider,
        authType: AuthType,
        preferredIDEs: List<IDEType>,
        isPrimary: Boolean
    ) -> Unit,
    isSaving: Boolean,
    error: String? = null,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(repository?.name ?: "") }
    var cloneUrl by remember { mutableStateOf(repository?.cloneUrl ?: "") }
    var provider by remember { mutableStateOf(repository?.provider ?: GitProvider.GITHUB) }
    var authType by remember { mutableStateOf(repository?.authType ?: AuthType.HTTPS) }
    var selectedIDEs by remember { mutableStateOf(repository?.preferredIDEs ?: emptyList()) }
    var isPrimary by remember { mutableStateOf(repository?.isPrimary ?: false) }
    
    val isEdit = repository != null
    val title = if (isEdit) "Edit Repository" else "Add Repository"
    
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                // Repository name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Repository Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    supportingText = { Text("A descriptive name for this repository") }
                )
                
                // Clone URL
                OutlinedTextField(
                    value = cloneUrl,
                    onValueChange = { cloneUrl = it },
                    label = { Text("Clone URL *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    singleLine = true,
                    supportingText = { Text("Git clone URL (HTTPS or SSH)") }
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
                        label = { Text("Authentication Type *") },
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
                        AuthType.values().forEach { auth ->
                            DropdownMenuItem(
                                text = { Text(auth.name) },
                                onClick = {
                                    authType = auth
                                    authExpanded = false
                                }
                            )
                        }
                    }
                }

                // IDE selection
                Text(
                    text = "Preferred IDEs *",
                    style = MaterialTheme.typography.labelMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IDEType.values().forEach { ide ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
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

                // Primary repository checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        enabled = !isSaving
                    )
                    Column {
                        Text(
                            text = "Set as Primary Repository",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "The primary repository is used as the default for tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, cloneUrl, provider, authType, selectedIDEs, isPrimary)
                },
                enabled = !isSaving && name.isNotBlank() && cloneUrl.isNotBlank() && selectedIDEs.isNotEmpty()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isEdit) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

