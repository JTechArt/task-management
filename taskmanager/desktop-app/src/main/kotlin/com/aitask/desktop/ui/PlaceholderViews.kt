package com.aitask.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        // Header
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
        
        // Placeholder card
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
                    text = "🚧 Coming Soon",
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
            .padding(24.dp),
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
                text = "Configure application settings, import/export data, and manage backups",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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
                        Icon(Icons.Default.Download, contentDescription = null)
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
                        Icon(Icons.Default.Upload, contentDescription = null)
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
                        Icon(Icons.Default.Backup, contentDescription = null)
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
                        Icon(Icons.Default.Upload, contentDescription = null)
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
        if (uiState.importFeedback != null) {
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearImportFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.importFeedback!!)
            }
        }
        if (uiState.restoreFeedback != null) {
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearRestoreFeedback() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(uiState.restoreFeedback!!)
            }
        }
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
                        text = uiState.error!!,
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
    }
}

