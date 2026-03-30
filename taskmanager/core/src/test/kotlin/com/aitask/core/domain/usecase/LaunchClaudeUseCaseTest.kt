package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ClaudeConfigurationRepository
import com.aitask.core.domain.repository.PreRunScriptRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.ClaudeAnthropicApiService
import com.aitask.core.domain.service.ClaudeCliService
import com.aitask.core.domain.service.PreRunScriptService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class LaunchClaudeUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var preRunScriptRepository: PreRunScriptRepository
    private lateinit var preRunScriptService: PreRunScriptService
    private lateinit var bmadConfigurationResolver: BmadConfigurationResolver
    private lateinit var claudeConfigurationRepository: ClaudeConfigurationRepository
    private lateinit var claudeCliService: ClaudeCliService
    private lateinit var claudeAnthropicApiService: ClaudeAnthropicApiService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: LaunchClaudeUseCase

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        preRunScriptRepository = mockk()
        preRunScriptService = mockk()
        bmadConfigurationResolver = BmadConfigurationResolver()
        claudeConfigurationRepository = mockk()
        claudeCliService = mockk()
        claudeAnthropicApiService = mockk()
        activityRepository = mockk()
        useCase = LaunchClaudeUseCase(
            taskRepository,
            projectRepository,
            preRunScriptRepository,
            preRunScriptService,
            bmadConfigurationResolver,
            claudeConfigurationRepository,
            claudeCliService,
            claudeAnthropicApiService,
            activityRepository
        )
    }

    @Test
    fun `fails when claude configuration is null`() = runTest {
        coEvery { claudeConfigurationRepository.find() } returns null
        val result = useCase(LaunchClaudeRequest(taskId = UUID.randomUUID()))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled") == true)
    }

    @Test
    fun `fails when claude integration is disabled`() = runTest {
        val configId = UUID.randomUUID()
        coEvery { claudeConfigurationRepository.find() } returns ClaudeConfiguration(
            id = configId,
            isEnabled = false,
            integrationMode = ClaudeIntegrationMode.CLI,
            cliPath = "",
            apiBaseUrl = null,
            hasApiKey = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val result = useCase(LaunchClaudeRequest(taskId = UUID.randomUUID()))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled") == true)
    }

    @Test
    fun `fails when task not found`() = runTest {
        val taskId = UUID.randomUUID()
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns null
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
    }

    @Test
    fun `fails when task has no workspace`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = minimalTask(taskId, projectId, workspacePath = null)
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("workspace") == true)
    }

    @Test
    fun `fails when project not found`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = minimalTask(taskId, projectId, workspacePath = "/tmp/ws")
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns null
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }

    @Test
    fun `fails when pre-run scripts fail`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("claude-prerun-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val scripts = listOf(
            PreRunScript(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = "fail",
                type = PreRunScriptType.INLINE_COMMAND,
                inlineScript = "false",
                executionOrder = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val failure = Exception("script boom")
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns scripts
        coEvery { preRunScriptService.executeScripts(scripts, task.workspacePath!!) } returns Result.failure(failure)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { claudeCliService.resolveExecutable(any()) }
        coVerify { activityRepository.create(match { it.type == ActivityType.PRE_RUN_FAILED }) }
    }

    @Test
    fun `fails when CLI resolve fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("claude-resolve-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("not on path")
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeConfigurationRepository.findApiKey(any()) } returns null
        coEvery { claudeCliService.resolveExecutable(null) } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
        coVerify { activityRepository.create(match { it.type == ActivityType.CLAUDE_LAUNCHED && it.status == ActivityStatus.FAILED }) }
    }

    @Test
    fun `fails when CLI validate fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("claude-validate-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("bad binary")
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeConfigurationRepository.findApiKey(any()) } returns null
        coEvery { claudeCliService.resolveExecutable(null) } returns Result.success("/bin/claude")
        coEvery { claudeCliService.validateExecutable("/bin/claude") } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
    }

    @Test
    fun `fails when terminal launch fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("claude-launch-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("no terminal")
        coEvery { claudeConfigurationRepository.find() } returns enabledClaudeCliConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeCliService.resolveExecutable(null) } returns Result.success("/bin/claude")
        coEvery { claudeCliService.validateExecutable("/bin/claude") } returns Result.success(Unit)
        coEvery { claudeConfigurationRepository.findApiKey(any()) } returns null
        coEvery { claudeCliService.launchInTerminal(any(), any(), any()) } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
    }

    @Test
    fun `fails when API sendTaskContextPrompt fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledClaudeApiConfig()
        val workspaceDir = Files.createTempDirectory("claude-api-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("HTTP 500")
        coEvery { claudeConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeConfigurationRepository.findApiKey(config.id) } returns "sk-ant"
        coEvery { claudeAnthropicApiService.sendTaskContextPrompt(any(), any(), any()) } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId, updateTaskStatus = false))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
        coVerify { activityRepository.create(match { it.type == ActivityType.CLAUDE_LAUNCHED && it.status == ActivityStatus.FAILED }) }
    }

    @Test
    fun `fails when API mode and no api key`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledClaudeApiConfig()
        val workspaceDir = Files.createTempDirectory("claude-api-no-key").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        coEvery { claudeConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeConfigurationRepository.findApiKey(config.id) } returns null
        val result = useCase(LaunchClaudeRequest(taskId = taskId, updateTaskStatus = false))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key") == true)
    }

    @Test
    fun `CLI success launches terminal and records activity`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledClaudeCliConfig()
        val workspaceDir = Files.createTempDirectory("claude-cli-ok").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        coEvery { claudeConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeCliService.resolveExecutable(null) } returns Result.success("/bin/claude")
        coEvery { claudeCliService.validateExecutable("/bin/claude") } returns Result.success(Unit)
        coEvery { claudeConfigurationRepository.findApiKey(config.id) } returns null
        coEvery { claudeCliService.launchInTerminal(any(), any(), any()) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId, updateTaskStatus = false))
        assertTrue(result.isSuccess)
        assertEquals("Launched Claude CLI successfully", result.getOrNull()?.userMessage)
        coVerify(atLeast = 1) { activityRepository.create(match { it.type == ActivityType.CLAUDE_LAUNCHED }) }
    }

    @Test
    fun `API success calls anthropic and records activity`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledClaudeApiConfig()
        val workspaceDir = Files.createTempDirectory("claude-api-ok").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        coEvery { claudeConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { claudeConfigurationRepository.findApiKey(config.id) } returns "sk-ant"
        coEvery { claudeAnthropicApiService.sendTaskContextPrompt(any(), any(), any()) } returns Result.success("ok")
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchClaudeRequest(taskId = taskId, updateTaskStatus = false))
        assertTrue(result.isSuccess)
        assertEquals("Claude API request completed successfully", result.getOrNull()?.userMessage)
        coVerify(atLeast = 1) { claudeAnthropicApiService.sendTaskContextPrompt(eq("sk-ant"), any(), any()) }
    }

    private fun enabledClaudeCliConfig(): ClaudeConfiguration {
        val id = UUID.randomUUID()
        return ClaudeConfiguration(
            id = id,
            isEnabled = true,
            integrationMode = ClaudeIntegrationMode.CLI,
            cliPath = "",
            apiBaseUrl = null,
            hasApiKey = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun enabledClaudeApiConfig(): ClaudeConfiguration {
        val id = UUID.randomUUID()
        return ClaudeConfiguration(
            id = id,
            isEnabled = true,
            integrationMode = ClaudeIntegrationMode.API,
            cliPath = "",
            apiBaseUrl = null,
            hasApiKey = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    private fun minimalTask(taskId: UUID, projectId: UUID, workspacePath: String?): Task = Task(
        id = taskId,
        title = "T",
        description = "D",
        taskType = TaskType.FEATURE,
        status = TaskStatus.PENDING,
        projectId = projectId,
        workspacePath = workspacePath,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )

    private fun minimalProject(projectId: UUID): Project = Project(
        id = projectId,
        name = "Proj",
        description = null,
        workspacePath = "/p",
        branchTemplate = "task-{taskId}",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
