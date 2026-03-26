package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.AgentExecutionService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RunAgentUseCaseTest {
    @Test
    fun `invoke renders task context and logs execution activity`() = runTest {
        val taskRepository = mockk<TaskRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()
        val agentDefinitionRepository = mockk<AgentDefinitionRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val agentExecutionService = mockk<AgentExecutionService>()
        val activityRepository = mockk<ActivityRepository>()

        val useCase = RunAgentUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            agentDefinitionRepository,
            llmConfigurationRepository,
            agentExecutionService,
            activityRepository
        )

        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val task = Task(
            id = taskId,
            title = "Add agent builder",
            description = "Create reusable agents",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = projectId,
            workspacePath = "/workspace",
            branchName = "feature/agent-builder",
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
        val agent = AgentDefinition(
            id = agentId,
            name = "Planner",
            description = "Draft action plans",
            promptTemplate = "Plan {{taskTitle}} for {{projectName}} using {{repositoryNames}}",
            llmConfigurationId = configId,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val configuration = LlmConfiguration(
            id = configId,
            name = "Local Agent",
            endpointUrl = "http://localhost:11434",
            modelIdentifier = "llama3.1",
            hasApiKey = false,
            isDefault = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repository)
        coEvery { agentDefinitionRepository.findById(agentId) } returns agent
        coEvery { llmConfigurationRepository.findById(configId) } returns configuration
        coEvery { agentExecutionService.execute(any(), any(), any()) } answers {
            val prompt = args[0] as String
            assertTrue(prompt.contains("Add agent builder"))
            assertTrue(prompt.contains("Atlas"))
            assertTrue(prompt.contains("desktop-app"))
            Result.success(
                AgentExecutionResult(
                    generatedText = "Plan drafted",
                    configurationName = configuration.name,
                    modelIdentifier = configuration.modelIdentifier,
                    endpointUrl = configuration.endpointUrl
                )
            )
        }
        coEvery { activityRepository.create(any()) } returns mockk()

        val result = useCase(RunAgentRequest(taskId, agentId))

        assertTrue(result.isSuccess)
        assertEquals("Plan drafted", result.getOrThrow().generatedText)
        coVerify(exactly = 1) { activityRepository.create(match { it.type == ActivityType.AGENT_EXECUTED }) }
    }

    @Test
    fun `invoke applies GEPPA optimization to rendered prompt when service is provided`() = runTest {
        val taskRepository = mockk<TaskRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()
        val agentDefinitionRepository = mockk<AgentDefinitionRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val agentExecutionService = mockk<AgentExecutionService>()
        val activityRepository = mockk<ActivityRepository>()
        val geppaOptimizationService = mockk<GeppaPromptOptimizationService>()

        val useCase = RunAgentUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            agentDefinitionRepository,
            llmConfigurationRepository,
            agentExecutionService,
            activityRepository,
            geppaOptimizationService
        )

        val projectId = UUID.randomUUID()
        val taskId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val configId = UUID.randomUUID()
        val task = Task(
            id = taskId,
            title = "Add agent builder",
            description = "Create reusable agents",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = projectId,
            workspacePath = "/workspace",
            branchName = "feature/agent-builder",
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
        val agent = AgentDefinition(
            id = agentId,
            name = "Planner",
            description = "Draft action plans",
            promptTemplate = "Plan {{taskTitle}} for {{projectName}}",
            llmConfigurationId = configId,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val configuration = LlmConfiguration(
            id = configId,
            name = "Local Agent",
            endpointUrl = "http://localhost:11434",
            modelIdentifier = "llama3.1",
            hasApiKey = false,
            isDefault = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { agentDefinitionRepository.findById(agentId) } returns agent
        coEvery { llmConfigurationRepository.findById(configId) } returns configuration
        coEvery { geppaOptimizationService.optimizeIfEnabled(any(), "agent_execution") } returns "GEPPA-optimized prompt"
        coEvery { agentExecutionService.execute("GEPPA-optimized prompt", any(), any()) } returns Result.success(
            AgentExecutionResult(
                generatedText = "Optimized plan drafted",
                configurationName = configuration.name,
                modelIdentifier = configuration.modelIdentifier,
                endpointUrl = configuration.endpointUrl
            )
        )
        coEvery { activityRepository.create(any()) } returns mockk()

        val result = useCase(RunAgentRequest(taskId, agentId))

        assertTrue(result.isSuccess)
        assertEquals("Optimized plan drafted", result.getOrThrow().generatedText)
        coVerify(exactly = 1) { geppaOptimizationService.optimizeIfEnabled(any(), "agent_execution") }
        coVerify(exactly = 1) {
            activityRepository.create(match {
                it.metadata["geppaOptimized"] == "true"
            })
        }
    }
}
