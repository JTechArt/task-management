package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.service.TaskContentGenerationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GenerateTaskContentUseCaseTest {
    @Test
    fun `invoke enriches request with project name and default llm profile`() = runTest {
        val projectRepository = mockk<ProjectRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val generationService = mockk<TaskContentGenerationService>()
        val useCase = GenerateTaskContentUseCase(projectRepository, llmConfigurationRepository, generationService)
        val projectId = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val request = TaskContentGenerationRequest(
            projectId = projectId,
            title = "Add local LLM generation",
            currentDescription = null,
            taskType = TaskType.FEATURE,
            mode = TaskContentGenerationMode.DESCRIPTION
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

        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { llmConfigurationRepository.findDefault() } returns configuration
        coEvery { llmConfigurationRepository.findApiKey(configId) } returns "secret-token"
        coEvery { generationService.generate(any(), any(), any()) } answers {
            val generatedRequest = args[0] as TaskContentGenerationRequest
            assertEquals("Atlas", generatedRequest.projectName)
            Result.success(
                TaskContentGenerationResult(
                    generatedText = "Generated task description",
                    configurationName = configuration.name,
                    modelIdentifier = configuration.modelIdentifier,
                    endpointUrl = configuration.endpointUrl
                )
            )
        }

        val result = useCase(request)

        assertTrue(result.isSuccess)
        assertEquals("Generated task description", result.getOrThrow().generatedText)
    }
}
