package com.aitask.desktop.ui.rules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleScope
import java.util.UUID

@Composable
fun CreateRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, RuleCategory?, RuleScope, IDEType?) -> Unit,
    isSaving: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<RuleCategory?>(null) }
    var scope by remember { mutableStateOf(RuleScope.GLOBAL) }
    var targetIDE by remember { mutableStateOf<IDEType?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Rule",
                    style = MaterialTheme.typography.headlineSmall
                )
                
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
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissError) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    enabled = !isSaving,
                    maxLines = 10
                )
                
                // Scope dropdown
                var scopeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = scopeExpanded,
                    onExpandedChange = { scopeExpanded = !scopeExpanded }
                ) {
                    OutlinedTextField(
                        value = scope.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scope") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = scopeExpanded,
                        onDismissRequest = { scopeExpanded = false }
                    ) {
                        RuleScope.entries.forEach { scopeOption ->
                            DropdownMenuItem(
                                text = { Text(scopeOption.name) },
                                onClick = {
                                    scope = scopeOption
                                    scopeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Category dropdown (optional)
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category?.name?.replace("_", " ") ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                category = null
                                categoryExpanded = false
                            }
                        )
                        RuleCategory.entries.forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(categoryOption.name.replace("_", " ")) },
                                onClick = {
                                    category = categoryOption
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target IDE dropdown (optional)
                var ideExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = ideExpanded,
                    onExpandedChange = { ideExpanded = !ideExpanded }
                ) {
                    OutlinedTextField(
                        value = targetIDE?.name ?: "None",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target IDE (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ideExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isSaving
                    )
                    ExposedDropdownMenu(
                        expanded = ideExpanded,
                        onDismissRequest = { ideExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                targetIDE = null
                                ideExpanded = false
                            }
                        )
                        IDEType.entries.forEach { ideOption ->
                            DropdownMenuItem(
                                text = { Text(ideOption.name) },
                                onClick = {
                                    targetIDE = ideOption
                                    ideExpanded = false
                                }
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
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
                            onConfirm(name, content, category, scope, targetIDE)
                        },
                        enabled = !isSaving && name.isNotBlank() && content.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditRuleDialog(
    rule: Rule,
    onDismiss: () -> Unit,
    onConfirm: (UUID, String?, String?, RuleCategory?, RuleScope?, IDEType?) -> Unit,
    isSaving: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    var name by remember { mutableStateOf(rule.name) }
    var content by remember { mutableStateOf(rule.content) }
    var category by remember { mutableStateOf(rule.category) }
    var scope by remember { mutableStateOf(rule.scope) }
    var targetIDE by remember { mutableStateOf(rule.targetIDE) }

    // Similar to CreateRuleDialog but with pre-filled values
    // Implementation omitted for brevity - follows same pattern
    Text("Edit Rule Dialog - Implementation similar to Create")
}

@Composable
fun ImportRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isSaving: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    var jsonContent by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Import Rules",
                    style = MaterialTheme.typography.headlineSmall
                )

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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissError) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss")
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = jsonContent,
                    onValueChange = { jsonContent = it },
                    label = { Text("JSON Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    enabled = !isSaving,
                    maxLines = 15
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(jsonContent) },
                        enabled = !isSaving && jsonContent.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Import")
                        }
                    }
                }
            }
        }
    }
}

