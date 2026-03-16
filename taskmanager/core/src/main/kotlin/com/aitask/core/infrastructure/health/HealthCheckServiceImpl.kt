package com.aitask.core.infrastructure.health

import com.aitask.core.domain.model.ComponentHealth
import com.aitask.core.domain.model.HealthState
import com.aitask.core.domain.model.HealthStatus
import com.aitask.core.domain.model.RepositoryHealth
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.service.GitAuthConfig
import com.aitask.core.domain.service.GitService
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.core.domain.service.IDEService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant

private val logger = KotlinLogging.logger {}

class HealthCheckServiceImpl(
    private val projectRepository: ProjectRepository,
    private val repositoryRepository: RepositoryRepository,
    private val gitService: GitService,
    private val ideService: IDEService
) : HealthCheckService {

    override suspend fun runHealthChecks(): HealthStatus = withContext(Dispatchers.IO) {
        val databaseHealth = checkDatabaseHealth()
        val (reposHealth, repoDetails) = checkRepositoriesHealth()
        val ideHealth = checkIDEDiscovery()
        HealthStatus(
            database = databaseHealth,
            repositories = reposHealth,
            repositoryDetails = repoDetails,
            ideDiscovery = ideHealth,
            lastRefreshedAt = Instant.now()
        )
    }

    private suspend fun checkDatabaseHealth(): ComponentHealth {
        return try {
            val start = System.currentTimeMillis()
            newSuspendedTransaction {
                exec("SELECT 1") { result -> result.next(); 1 }
            }
            val elapsed = System.currentTimeMillis() - start
            ComponentHealth(
                id = "database",
                label = "Database",
                state = HealthState.HEALTHY,
                message = "Connected",
                detail = "$elapsed ms"
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Connection failed"
            logger.warn(e) { "Database health check failed: $errorMessage" }
            ComponentHealth(
                id = "database",
                label = "Database",
                state = HealthState.FAILED,
                message = "Not connected",
                detail = errorMessage
            )
        }
    }

    private suspend fun checkRepositoriesHealth(): Pair<ComponentHealth, List<RepositoryHealth>> {
        val projects = try {
            projectRepository.findAllActive()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load projects for repo health check" }
            return Pair(
                ComponentHealth(
                    id = "repositories",
                    label = "Repositories",
                    state = HealthState.FAILED,
                    message = "Cannot load projects",
                    detail = e.message
                ),
                emptyList()
            )
        }
        val allRepos = projects.flatMap { project ->
            try {
                repositoryRepository.findByProject(project.id).map { repo -> project to repo }
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (allRepos.isEmpty()) {
            return Pair(
                ComponentHealth(
                    id = "repositories",
                    label = "Repositories",
                    state = HealthState.HEALTHY,
                    message = "No repositories configured",
                    detail = "Add repositories to projects to validate remote access"
                ),
                emptyList()
            )
        }
        val details = allRepos.map { (project, repo) ->
            val authConfig = GitAuthConfig(
                authType = repo.authType,
                username = null,
                password = null,
                sshKeyPath = null,
                token = null
            )
            val result = gitService.validateRemoteRepository(repo.cloneUrl, authConfig)
            val info = result.getOrNull() ?: com.aitask.core.domain.service.RepositoryInfo(
                url = repo.cloneUrl,
                isAccessible = false,
                errorMessage = "Validation failed"
            )
            val state = if (info.isAccessible) HealthState.HEALTHY else HealthState.FAILED
            val message = when {
                info.isAccessible -> "Accessible"
                !info.errorMessage.isNullOrBlank() -> info.errorMessage
                else -> "Cannot access repository"
            }
            RepositoryHealth(
                repositoryId = repo.id,
                repositoryName = repo.name,
                projectName = project.name,
                cloneUrl = repo.cloneUrl,
                state = state,
                message = message
            )
        }
        val healthyCount = details.count { it.state == HealthState.HEALTHY }
        val totalCount = details.size
        val failedCount = details.count { it.state == HealthState.FAILED }
        val reposState = when {
            failedCount == 0 -> HealthState.HEALTHY
            healthyCount == 0 -> HealthState.FAILED
            else -> HealthState.DEGRADED
        }
        val reposMessage = when (reposState) {
            HealthState.HEALTHY -> "$healthyCount of $totalCount repositories accessible"
            HealthState.DEGRADED -> "$healthyCount of $totalCount repositories accessible; $failedCount issue(s)"
            HealthState.FAILED -> "$failedCount of $totalCount repositories failed"
            else -> "$totalCount repositories configured"
        }
        val reposDetail = when {
            failedCount > 0 -> details.filter { it.state == HealthState.FAILED }
                .take(3)
                .joinToString("; ") { "${it.repositoryName}: ${it.message}" }
            else -> null
        }
        return Pair(
            ComponentHealth(
                id = "repositories",
                label = "Repositories",
                state = reposState,
                message = reposMessage,
                detail = reposDetail
            ),
            details
        )
    }

    private suspend fun checkIDEDiscovery(): ComponentHealth {
        val result = try {
            val ides = ideService.detectInstalledIDEs()
            val count = ides.size
            val state = if (count > 0) HealthState.HEALTHY else HealthState.DEGRADED
            val message = when (count) {
                0 -> "No IDEs detected"
                1 -> "1 IDE detected"
                else -> "$count IDEs detected"
            }
            val detail = if (ides.isNotEmpty()) {
                ides.map { it.type.name }.joinToString(", ")
            } else {
                "Install Cursor, VS Code, or JetBrains IDEs for workspace launch"
            }
            ComponentHealth(
                id = "ide-discovery",
                label = "IDE discovery",
                state = state,
                message = message,
                detail = detail
            )
        } catch (e: Exception) {
            ComponentHealth(
                id = "ide-discovery",
                label = "IDE discovery",
                state = HealthState.FAILED,
                message = "Check failed",
                detail = e.message ?: "Unknown error"
            )
        }
        return result
    }
}
