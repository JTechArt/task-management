package com.aitask.desktop.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.desktop.ui.NavigationItem
import com.aitask.desktop.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardView(
    viewModel: DashboardViewModel = remember { DashboardViewModel() },
    databaseConnected: Boolean,
    onNavigate: (NavigationItem) -> Unit,
    onNavigateToTask: (taskId: java.util.UUID) -> Unit
) {
    LaunchedEffect(databaseConnected) {
        viewModel.loadDashboard(databaseConnected)
    }
    val uiState = viewModel.uiState
    val hasData = uiState.metrics.projectsCount > 0 || uiState.metrics.totalTasksCount > 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DashboardHeader(
            onOpenLastWorkspace = { onNavigate(NavigationItem.TASKS) },
            onViewAllTasks = { onNavigate(NavigationItem.TASKS) }
        )
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (!hasData) {
            DashboardEmptyState(onNavigate = onNavigate)
        } else {
            DashboardMetricsSection(metrics = uiState.metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                QuickOpenSection(
                    recentTasks = uiState.recentTasks,
                    findProjectName = { viewModel.findProjectName(it) },
                    onTaskClick = { onNavigateToTask(it.id) },
                    onManageQueue = { onNavigate(NavigationItem.TASKS) },
                    modifier = Modifier.weight(1f)
                )
                SystemHealthSection(
                    databaseConnected = databaseConnected,
                    onOpenDiagnostics = { onNavigate(NavigationItem.INTEGRATIONS) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                RecentActivitySection(
                    activities = uiState.recentActivity,
                    onViewAll = { onNavigate(NavigationItem.ACTIVITY) },
                    modifier = Modifier.weight(1f)
                )
                ProjectsInMotionSection(
                    projects = uiState.projects,
                    recentTasks = uiState.recentTasks,
                    onNavigateToProjects = { onNavigate(NavigationItem.PROJECTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    onOpenLastWorkspace: () -> Unit,
    onViewAllTasks: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today at a glance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Start work without hunting for context",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The dashboard prioritizes orientation and momentum: favorite tasks, active projects, recent workspace events, and system readiness.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Row(
            modifier = Modifier.padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onOpenLastWorkspace) {
                Text("Open last workspace")
            }
            OutlinedButton(onClick = onViewAllTasks) {
                Text("View all tasks")
            }
        }
    }
}

@Composable
private fun DashboardMetricsSection(metrics: com.aitask.desktop.ui.viewmodel.DashboardMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "Open tasks",
            value = metrics.openTasksCount.toString(),
            subtitle = "In progress + pending",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Total tasks",
            value = metrics.totalTasksCount.toString(),
            subtitle = "All statuses",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Projects",
            value = metrics.projectsCount.toString(),
            subtitle = "Active projects",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Rules attached",
            value = metrics.rulesCount.toString(),
            subtitle = "Available for IDE",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun QuickOpenSection(
    recentTasks: List<Task>,
    findProjectName: (java.util.UUID) -> String,
    onTaskClick: (Task) -> Unit,
    onManageQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = "Quick open",
        subtitle = "Pin recent tasks and jump directly to launch",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {}
            TextButton(onClick = onManageQueue) {
                Text("Manage queue")
            }
        }
        if (recentTasks.isEmpty()) {
            Text(
                text = "No open tasks. Create a task to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentTasks) { task ->
                    QuickOpenRow(
                        task = task,
                        projectName = findProjectName(task.projectId),
                        onClick = { onTaskClick(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickOpenRow(
    task: Task,
    projectName: String,
    onClick: () -> Unit
) {
    val badgeLabel = when (task.status) {
        TaskStatus.IN_PROGRESS -> "In progress"
        TaskStatus.PENDING -> "Ready"
        else -> "Open"
    }
    val badgeColor = when (task.status) {
        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
        TaskStatus.PENDING -> Color(0xFF4CAF50)
        else -> Color(0xFF9E9E9E)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$projectName • ${task.taskType.name.replace('_', ' ')}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Surface(
            color = badgeColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = badgeLabel,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun SystemHealthSection(
    databaseConnected: Boolean,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = "System health",
        subtitle = "High-signal statuses only",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {}
            TextButton(onClick = onOpenDiagnostics) {
                Text("Open diagnostics")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (databaseConnected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Surface(
                color = if (databaseConnected) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (databaseConnected) "Healthy" else "Action",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (databaseConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
private fun RecentActivitySection(
    activities: List<Activity>,
    onViewAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    DashboardCard(
        title = "Recent activity",
        subtitle = "Workspace opens, task status changes, and setup events",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {}
            TextButton(onClick = onViewAll) {
                Text("View activity")
            }
        }
        if (activities.isEmpty()) {
            Text(
                text = "No recent activity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(activities) { activity ->
                    val statusColor = when (activity.status) {
                        com.aitask.core.domain.model.ActivityStatus.SUCCESS -> Color(0xFF2E7D32)
                        com.aitask.core.domain.model.ActivityStatus.FAILED -> Color(0xFFC62828)
                        com.aitask.core.domain.model.ActivityStatus.IN_PROGRESS -> Color(0xFF1565C0)
                    }
                    val backgroundColor = when (activity.status) {
                        com.aitask.core.domain.model.ActivityStatus.SUCCESS -> Color(0xFFE8F5E9)
                        com.aitask.core.domain.model.ActivityStatus.FAILED -> Color(0xFFFFEBEE)
                        com.aitask.core.domain.model.ActivityStatus.IN_PROGRESS -> Color(0xFFE3F2FD)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(backgroundColor)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activity.description,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activity.type.name.replace('_', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Surface(
                                    color = statusColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = activity.status.name.replace('_', ' '),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatRelativeTime(activity.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(instant: java.time.Instant): String {
    val ago = java.time.Duration.between(instant, java.time.Instant.now())
    return when {
        ago.toMinutes() < 1 -> "Just now"
        ago.toMinutes() < 60 -> "${ago.toMinutes()}m ago"
        ago.toHours() < 24 -> "${ago.toHours()}h ago"
        else -> "${ago.toDays()}d ago"
    }
}

@Composable
private fun ProjectsInMotionSection(
    projects: List<com.aitask.core.domain.model.Project>,
    recentTasks: List<Task>,
    onNavigateToProjects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksByProject = recentTasks.groupBy { it.projectId }
    DashboardCard(
        title = "Projects in motion",
        subtitle = "Active work by project",
        modifier = modifier
    ) {
        if (projects.isEmpty()) {
            Text(
                text = "No projects. Create a project to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                projects.take(5).forEach { project ->
                    val activeCount = tasksByProject[project.id]?.count { it.isActive } ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable(onClick = onNavigateToProjects)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$activeCount open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun DashboardEmptyState(onNavigate: (NavigationItem) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to AiTask",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                text = "Get started by creating your first project and task. The dashboard will show your active work, recent activity, and quick navigation shortcuts.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1976D2).copy(alpha = 0.9f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onNavigate(NavigationItem.PROJECTS) }) {
                    Text("Create project")
                }
                OutlinedButton(onClick = { onNavigate(NavigationItem.TASKS) }) {
                    Text("View tasks")
                }
            }
        }
    }
}
