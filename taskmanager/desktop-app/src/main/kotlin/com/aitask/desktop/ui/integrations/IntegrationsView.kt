package com.aitask.desktop.ui.integrations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitask.core.domain.model.ComponentHealth
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.model.HealthStatus
import com.aitask.core.domain.model.RepositoryHealth
import com.aitask.desktop.ui.viewmodel.IntegrationsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HEALTHY_COLOR = Color(0xFF2E7D32)
private val DEGRADED_COLOR = Color(0xFFF9A825)
private val FAILED_COLOR = Color(0xFFC62828)
private val UNKNOWN_COLOR = Color(0xFF757575)

@Composable
fun IntegrationsView(
    viewModel: IntegrationsViewModel = remember { IntegrationsViewModel() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.loadHealth()
    }
    val healthStatus by viewModel.healthStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IntegrationsHeader(
            onRefresh = { viewModel.refreshHealth() },
            isLoading = isLoading,
            lastRefreshedAt = healthStatus?.lastRefreshedAt
        )
        error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = err,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        if (isLoading && healthStatus == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            healthStatus?.let { status ->
                ConnectionHealthCard(status = status)
            }
        }
    }
}

@Composable
private fun IntegrationsHeader(
    onRefresh: () -> Unit,
    isLoading: Boolean,
    lastRefreshedAt: Instant?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Connections and status",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = "Integrations & Health",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Monitor database, repositories, and IDE discovery. Health checks run on demand and do not block browsing of local data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            lastRefreshedAt?.let { at ->
                Text(
                    text = "Last refreshed: ${formatLastRefreshed(at)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } ?: Spacer(Modifier)
            Button(
                onClick = onRefresh,
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Checking…" else "Run health checks")
            }
        }
    }
}

@Composable
private fun ConnectionHealthCard(
    status: HealthStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Connection health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Database, Git repositories, and IDE discovery status",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            HealthRow(component = status.database)
            HealthRow(component = status.repositories, detail = status.repositories.detail)
            status.repositoryDetails.take(5).forEach { repo ->
                RepositoryHealthRow(repo = repo)
            }
            HealthRow(component = status.ideDiscovery)
        }
    }
}

@Composable
private fun HealthRow(
    component: ComponentHealth,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    val (badgeColor, badgeBg) = badgeColorsForState(component.state)
    val displayDetail = detail ?: component.detail
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = component.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = buildActionableMessage(component),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            displayDetail?.let { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = badgeTextForComponent(component),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RepositoryHealthRow(
    repo: RepositoryHealth,
    modifier: Modifier = Modifier
) {
    val (badgeColor, badgeBg) = badgeColorsForState(repo.state)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${repo.repositoryName} (${repo.projectName})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = repo.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = badgeTextForState(repo.state),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

private fun badgeColorsForState(state: HealthState): Pair<Color, Color> = when (state) {
    HealthState.HEALTHY -> HEALTHY_COLOR to HEALTHY_COLOR.copy(alpha = 0.2f)
    HealthState.DEGRADED -> DEGRADED_COLOR to DEGRADED_COLOR.copy(alpha = 0.2f)
    HealthState.FAILED -> FAILED_COLOR to FAILED_COLOR.copy(alpha = 0.2f)
    HealthState.UNKNOWN -> UNKNOWN_COLOR to UNKNOWN_COLOR.copy(alpha = 0.2f)
}

private fun badgeTextForComponent(component: ComponentHealth): String = when (component.state) {
    HealthState.HEALTHY -> component.detail?.takeIf { it.length <= 12 } ?: "Healthy"
    HealthState.DEGRADED -> "Degraded"
    HealthState.FAILED -> "Failed"
    HealthState.UNKNOWN -> "Unknown"
}

private fun badgeTextForState(state: HealthState): String = when (state) {
    HealthState.HEALTHY -> "Healthy"
    HealthState.DEGRADED -> "Degraded"
    HealthState.FAILED -> "Failed"
    HealthState.UNKNOWN -> "Unknown"
}

private fun buildActionableMessage(component: ComponentHealth): String {
    return when (component.state) {
        HealthState.FAILED -> when (component.id) {
            "database" -> "Database connection failed. Verify PostgreSQL is running, check connection string in environment, and ensure migrations have been applied."
            "repositories" -> component.message + if (!component.detail.isNullOrBlank()) " Check repository credentials and remote URLs in Projects." else ""
            "ide-discovery" -> "IDE detection failed. Ensure Cursor, VS Code, or JetBrains IDEs are installed and executable paths are correct."
            else -> component.message
        }
        HealthState.DEGRADED -> when (component.id) {
            "ide-discovery" -> "No IDEs detected. Install Cursor, VS Code, or JetBrains IDEs for workspace launch."
            else -> component.message
        }
        else -> component.message
    }
}

private fun formatLastRefreshed(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
