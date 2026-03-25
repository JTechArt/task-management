package com.aitask.core.infrastructure.mcp

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID

class DefaultMcpBridgeServiceTest {
    @Test
    fun `sync starts local bridge and exposes sanitized context`() = runTest {
        val port = findFreePort()
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()

        val settingsRepository = mockk<McpServerConfigurationRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val taskRepository = mockk<TaskRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()

        val project = Project(
            id = projectId,
            name = "Atlas",
            description = "Primary app",
            workspacePath = "/workspace/atlas",
            branchTemplate = "task-{taskId}",
            methodology = Methodology.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val task = Task(
            id = taskId,
            title = "Build MCP bridge",
            description = "Expose safe task context",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = projectId,
            workspacePath = "/workspace/atlas",
            branchName = "feature/mcp",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val repository = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "atlas-api",
            cloneUrl = "https://token@example.com/atlas.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.TOKEN,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { settingsRepository.find() } returns McpServerConfiguration(
            id = UUID.randomUUID(),
            isEnabled = true,
            port = port,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { projectRepository.findAllActive() } returns listOf(project)
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { taskRepository.findByProject(projectId) } returns listOf(task)
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repository)

        val service = DefaultMcpBridgeService(
            settingsRepository = settingsRepository,
            projectRepository = projectRepository,
            taskRepository = taskRepository,
            repositoryRepository = repositoryRepository
        )

        val manifest = service.sync()
        assertTrue(manifest.endpointUrl.contains(":$port/mcp"))
        assertTrue(manifest.safetyNotes.any { it.contains("loopback") })

        val client = HttpClient.newHttpClient()
        val contextResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:$port/mcp/context?projectId=$projectId"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertTrue(contextResponse.body().contains("Build MCP bridge"))
        assertTrue(contextResponse.body().contains("/workspace/atlas"))
        assertTrue(!contextResponse.body().contains("cloneUrl"))
        assertTrue(!contextResponse.body().contains("token@example.com"))

        val manifestResponse = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:$port/mcp/manifest"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
        assertTrue(manifestResponse.body().contains("AiTask MCP bridge"))

        service.stop()
    }

    private fun findFreePort(): Int =
        ServerSocket(0).use { socket -> socket.localPort }
}
