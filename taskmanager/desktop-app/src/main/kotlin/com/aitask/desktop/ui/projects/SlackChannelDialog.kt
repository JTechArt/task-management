package com.aitask.desktop.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.TaskEvent

@Composable
fun SlackChannelDialog(
    existing: SlackChannelConfig?,
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: (webhookUrl: String, channelDisplayName: String, enabledEvents: Set<TaskEvent>) -> Unit,
    isSaving: Boolean = false,
    error: String? = null,
    onDismissError: () -> Unit = {}
) {
    var webhookUrl by remember(existing) { mutableStateOf(existing?.webhookUrl ?: "") }
    var channelDisplayName by remember(existing) { mutableStateOf(existing?.channelDisplayName ?: "") }
    var enabledEvents by remember(existing) { mutableStateOf(existing?.enabledEvents ?: emptySet<TaskEvent>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Slack channel" else "Edit Slack channel") },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Webhook URL") },
                    placeholder = { Text("https://hooks.slack.com/services/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = channelDisplayName,
                    onValueChange = { channelDisplayName = it },
                    label = { Text("Channel display name") },
                    placeholder = { Text("#project-updates") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "Events that trigger notifications:",
                    style = MaterialTheme.typography.labelLarge
                )
                TaskEvent.entries.forEach { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enabledEvents.contains(event),
                            onCheckedChange = { checked ->
                                enabledEvents = if (checked) {
                                    enabledEvents + event
                                } else {
                                    enabledEvents - event
                                }
                            }
                        )
                        Text(
                            text = event.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(webhookUrl.trim(), channelDisplayName.trim(), enabledEvents)
                },
                enabled = !isSaving &&
                    webhookUrl.isNotBlank() &&
                    channelDisplayName.isNotBlank() &&
                    webhookUrl.matches(Regex("^https://hooks\\.slack\\.com/services/.*"))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
