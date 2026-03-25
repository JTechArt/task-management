package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.GitAssistantSuggestionService
import com.aitask.core.domain.service.GitService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GenerateGitAssistantSuggestionUseCaseTest {
    @Test
    fun `invoke enriches request with task project and repository context`() = runTest {
        val taskRepository = mockk<TaskRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val gitService = mockk<GitService>()
        val service = mockk<GitAssistantSuggestionService>()
        val useCase = GenerateGitAssistantSuggestionUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            llmConfigurationRepository,
            gitService,
            service
        )
        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val task = Task(
            id = taskId,
            title = "Add git assistant",
            description = "Draft commit message and PR text",
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = "/workspace",
            branchName = "feature/git-assist",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val project = Project(
            id = projectId,
            name = "Atlas",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            methodology = Methodology.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val configuration = LlmConfiguration(
            id = configId,
            name = "Local Ollama",
            endpointUrl = "http://localhost:11434",
            modelIdentifier = "llama3.1",
            hasApiKey = true,
            isDefault = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val repository = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "desktop-app",
            cloneUrl = "https://example.com/desktop-app.git",
            provider = GitProvider.OTHER,
            authType = AuthType.HTTPS,
            preferredIDEs = emptyList(),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repository)
        coEvery { llmConfigurationRepository.findDefault() } returns configuration
        coEvery { llmConfigurationRepository.findApiKey(configId) } returns "secret-token"
        coEvery { gitService.getStagedChangesSummary("/workspace") } returns Result.success(
            """
            Staged files:
            - src/main/kotlin/com/example/GitAssistant.kt
            Staged diff summary:
            - MODIFY: src/main/kotlin/com/example/GitAssistant.kt -> src/main/kotlin/com/example/GitAssistant.kt
            """.trimIndent()
        )
        coEvery { service.generate(any(), any(), any()) } answers {
            val generatedRequest = args[0] as GitAssistantSuggestionRequest
            assertEquals("Atlas", generatedRequest.projectName)
            assertEquals("feature/git-assist", generatedRequest.branchName)
            assertEquals(listOf("desktop-app"), generatedRequest.repositoryNames)
            assertTrue(generatedRequest.stagedChangesSummary?.contains("GitAssistant.kt") == true)
            Result.success(
                GitAssistantSuggestionResult(
                    generatedText = "feat: add git assistant",
                    configurationName = configuration.name,
                    modelIdentifier = configuration.modelIdentifier,
                    endpointUrl = configuration.endpointUrl
                )
            )
        }

        val result = useCase(GenerateGitAssistantSuggestionRequest(taskId, GitAssistantSuggestionMode.COMMIT_MESSAGE))

        assertTrue(result.isSuccess)
        assertEquals("feat: add git assistant", result.getOrThrow().generatedText)
    }
}
