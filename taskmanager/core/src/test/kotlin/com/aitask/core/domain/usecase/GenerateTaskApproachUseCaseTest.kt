package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.AgentDefinitionRepository
import com.aitask.core.domain.repository.LlmConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.TaskApproachGenerationLlmOutput
import com.aitask.core.domain.service.TaskApproachGenerationService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GenerateTaskApproachUseCaseTest {
    private val taskId: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
    private val projectId: UUID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff")
    private val agentId: UUID = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000001")
    private val llmId: UUID = UUID.fromString("dddddddd-eeee-ffff-0000-111111111111")

    @Test
    fun `invoke returns approach when service succeeds`() = runTest {
        val taskRepository = mockk<TaskRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()
        val agentDefinitionRepository = mockk<AgentDefinitionRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val service = mockk<TaskApproachGenerationService>()
        val now = Instant.now()
        val task = Task(
            id = taskId,
            title = "Fix login",
            description = "OAuth fails",
            taskType = TaskType.BUG_FIX,
            status = TaskStatus.IN_PROGRESS,
            projectId = projectId,
            workspacePath = null,
            branchName = null,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        val project = Project(
            id = projectId,
            name = "Portal",
            description = null,
            workspacePath = "/w",
            branchTemplate = "t",
            methodology = Methodology.NONE,
            createdAt = now,
            updatedAt = now
        )
        val agent = AgentDefinition(
            id = agentId,
            name = "Bug agent",
            description = null,
            promptTemplate = "x",
            llmConfigurationId = llmId,
            associatedTools = setOf(AgentToolKind.LOCAL_LLM, AgentToolKind.CODEX),
            taskTypeFilter = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = now,
            updatedAt = now
        )
        val configuration = LlmConfiguration(
            id = llmId,
            name = "Local",
            endpointUrl = "http://localhost:11434",
            modelIdentifier = "m",
            hasApiKey = false,
            isDefault = true,
            createdAt = now,
            updatedAt = now
        )
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { agentDefinitionRepository.findAvailableForProject(projectId, TaskType.BUG_FIX) } returns listOf(agent)
        coEvery { llmConfigurationRepository.findById(llmId) } returns configuration
        coEvery { llmConfigurationRepository.findApiKey(any()) } returns null
        val step = TaskApproachStep(
            title = "Reproduce",
            detail = "Run login",
            suggestedTools = listOf(AgentToolKind.LOCAL_LLM),
            prompt = "Check logs"
        )
        coEvery {
            service.generate(any(), any(), any())
        } returns Result.success(
            TaskApproachGenerationLlmOutput(
                summary = "Plan",
                steps = listOf(step),
                configurationName = "Local",
                modelIdentifier = "m",
                endpointUrl = "http://localhost:11434/api/generate"
            )
        )
        val useCase = GenerateTaskApproachUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            agentDefinitionRepository,
            llmConfigurationRepository,
            service
        )
        val result = useCase(GenerateTaskApproachRequest(taskId = taskId, agentId = agentId))
        assertTrue(result.isSuccess)
        val approach = result.getOrNull()!!.approach
        assertEquals("Plan", approach.summary)
        assertEquals(1, approach.steps.size)
        assertEquals(agentId, approach.contextAgentId)
        assertEquals("Bug agent", approach.contextAgentName)
    }

    @Test
    fun `invoke fails when explicit agent is not available for task`() = runTest {
        val taskRepository = mockk<TaskRepository>()
        val projectRepository = mockk<ProjectRepository>()
        val repositoryRepository = mockk<RepositoryRepository>()
        val agentDefinitionRepository = mockk<AgentDefinitionRepository>()
        val llmConfigurationRepository = mockk<LlmConfigurationRepository>()
        val service = mockk<TaskApproachGenerationService>()
        val now = Instant.now()
        val task = Task(
            id = taskId,
            title = "T",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = null,
            branchName = null,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        val project = Project(
            id = projectId,
            name = "P",
            description = null,
            workspacePath = "/w",
            branchTemplate = "t",
            methodology = Methodology.NONE,
            createdAt = now,
            updatedAt = now
        )
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { agentDefinitionRepository.findAvailableForProject(projectId, TaskType.FEATURE) } returns emptyList()
        val useCase = GenerateTaskApproachUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            agentDefinitionRepository,
            llmConfigurationRepository,
            service
        )
        val result = useCase(GenerateTaskApproachRequest(taskId = taskId, agentId = agentId))
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("not available", ignoreCase = true) == true
        )
    }
}
