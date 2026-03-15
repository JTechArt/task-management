package com.aitask.desktop.ui.rules

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
import com.aitask.core.domain.model.Rule
import com.aitask.desktop.ui.viewmodel.RulesViewModel

@Composable
fun RulesView(
    modifier: Modifier = Modifier,
    viewModel: RulesViewModel = remember { RulesViewModel() }
) {
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Rules",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage reusable rule sets for IDE and AI tools",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.showImportDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import")
                }
                
                Button(
                    onClick = { viewModel.showCreateDialog() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Rule")
                }
            }
        }
        
        // Export feedback (success)
        if (uiState.exportFeedback != null) {
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearExportFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.exportFeedback!!)
            }
        }
        
        // Error message
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
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
        } else if (uiState.rules.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No rules yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create your first rule to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { viewModel.showCreateDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Rule")
                    }
                }
            }
        } else {
            // Rules list
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rules list (left side)
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.rules) { rule ->
                            RuleListItem(
                                rule = rule,
                                isSelected = rule.id == uiState.selectedRule?.id,
                                onClick = { viewModel.selectRule(rule) }
                            )
                        }
                    }
                }

                // Rule detail (right side)
                if (uiState.selectedRule != null) {
                    Card(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    ) {
                        RuleDetailView(
                            rule = uiState.selectedRule,
                            projects = uiState.projects,
                            attachedProjectIds = uiState.attachedProjectIds,
                            onEdit = { viewModel.showEditDialog(it) },
                            onDelete = { viewModel.deleteRule(it) },
                            onAttach = { projectId, ruleId -> viewModel.attachRule(projectId, ruleId) },
                            onDetach = { projectId, ruleId -> viewModel.detachRule(projectId, ruleId) },
                            onExport = { ruleId -> viewModel.exportRules(listOf(ruleId)) }
                        )
                    }
                }
            }
        }
    }

    // Create rule dialog
    if (uiState.showCreateDialog) {
        CreateRuleDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onConfirm = { name, content, category, scope, targetIDE ->
                viewModel.createRule(name, content, category, scope, targetIDE)
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }

    // Edit rule dialog
    if (uiState.showEditDialog && uiState.editingRule != null) {
        EditRuleDialog(
            rule = uiState.editingRule,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { id, name, content, category, scope, targetIDE ->
                viewModel.updateRule(id, name, content, category, scope, targetIDE)
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }

    // Import dialog
    if (uiState.showImportDialog) {
        ImportRuleDialog(
            onDismiss = { viewModel.hideImportDialog() },
            onConfirm = { jsonContent ->
                viewModel.importRules(jsonContent)
            },
            isSaving = uiState.isSaving,
            error = uiState.error,
            onDismissError = { viewModel.clearError() }
        )
    }
}

