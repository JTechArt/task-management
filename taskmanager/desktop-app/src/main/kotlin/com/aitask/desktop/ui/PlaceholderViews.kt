package com.aitask.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

@Composable
fun ProjectsView(modifier: Modifier = Modifier) {
    PlaceholderView(
        title = "Projects",
        description = "Manage multi-repository projects and workspace configurations",
        modifier = modifier
    )
}

@Composable
fun TasksView(modifier: Modifier = Modifier) {
    PlaceholderView(
        title = "Tasks",
        description = "Browse and manage your task queue",
        modifier = modifier
    )
}

@Composable
fun RulesView(modifier: Modifier = Modifier) {
    PlaceholderView(
        title = "Rules",
        description = "Create and manage reusable rule sets for IDE and AI tools",
        modifier = modifier
    )
}

@Composable
fun IntegrationsView(modifier: Modifier = Modifier) {
    PlaceholderView(
        title = "Integrations",
        description = "Manage credentials, OAuth connections, and view system health",
        modifier = modifier
    )
}

@Composable
fun SettingsView(modifier: Modifier = Modifier) {
    PlaceholderView(
        title = "Settings",
        description = "Configure application settings, import/export data, and manage backups",
        modifier = modifier
    )
}

