package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.CodexConfigurationRepository
import com.aitask.core.domain.repository.PreRunScriptRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.CodexCliService
import com.aitask.core.domain.service.PreRunScriptService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class LaunchCodexUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var preRunScriptRepository: PreRunScriptRepository
    private lateinit var preRunScriptService: PreRunScriptService
    private lateinit var bmadConfigurationResolver: BmadConfigurationResolver
    private lateinit var codexConfigurationRepository: CodexConfigurationRepository
    private lateinit var codexCliService: CodexCliService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: LaunchCodexUseCase

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        preRunScriptRepository = mockk()
        preRunScriptService = mockk()
        bmadConfigurationResolver = BmadConfigurationResolver()
        codexConfigurationRepository = mockk()
        codexCliService = mockk()
        activityRepository = mockk()
        useCase = LaunchCodexUseCase(
            taskRepository,
            projectRepository,
            preRunScriptRepository,
            preRunScriptService,
            bmadConfigurationResolver,
            codexConfigurationRepository,
            codexCliService,
            activityRepository
        )
    }

    @Test
    fun `fails when codex configuration is null`() = runTest {
        coEvery { codexConfigurationRepository.find() } returns null
        val result = useCase(LaunchCodexRequest(taskId = UUID.randomUUID()))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled") == true)
    }

    @Test
    fun `fails when codex integration is disabled`() = runTest {
        val configId = UUID.randomUUID()
        coEvery { codexConfigurationRepository.find() } returns CodexConfiguration(
            id = configId,
            isEnabled = false,
            cliPath = "",
            hasApiKey = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val result = useCase(LaunchCodexRequest(taskId = UUID.randomUUID()))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled") == true)
    }

    @Test
    fun `fails when task not found`() = runTest {
        val taskId = UUID.randomUUID()
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns null
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
    }

    @Test
    fun `fails when task has no workspace`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = minimalTask(taskId, projectId, workspacePath = null)
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("workspace") == true)
    }

    @Test
    fun `fails when project not found`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = minimalTask(taskId, projectId, workspacePath = "/tmp/ws")
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns null
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }

    @Test
    fun `fails when pre-run scripts fail`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("codex-prerun-fail").toFile()
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
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns scripts
        coEvery { preRunScriptService.executeScripts(scripts, task.workspacePath!!) } returns Result.failure(failure)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { codexCliService.resolveExecutable(any()) }
        coVerify { activityRepository.create(match { it.type == ActivityType.PRE_RUN_FAILED }) }
    }

    @Test
    fun `fails when codex resolve fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("codex-resolve-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("not on path")
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { codexCliService.resolveExecutable(null) } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
        coVerify { activityRepository.create(match { it.type == ActivityType.CODEX_LAUNCHED && it.status == ActivityStatus.FAILED }) }
    }

    @Test
    fun `fails when codex validate fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("codex-validate-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("bad binary")
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { codexCliService.resolveExecutable(null) } returns Result.success("/bin/codex")
        coEvery { codexCliService.validateExecutable("/bin/codex") } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
    }

    @Test
    fun `fails when terminal launch fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("codex-launch-fail").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val err = IllegalStateException("no terminal")
        coEvery { codexConfigurationRepository.find() } returns enabledCodexConfig()
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { codexCliService.resolveExecutable(null) } returns Result.success("/bin/codex")
        coEvery { codexCliService.validateExecutable("/bin/codex") } returns Result.success(Unit)
        coEvery { codexConfigurationRepository.findApiKey(any()) } returns null
        coEvery { codexCliService.launchInTerminal(any(), any(), any()) } returns Result.failure(err)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId))
        assertTrue(result.isFailure)
        assertSame(err, result.exceptionOrNull())
    }

    @Test
    fun `launches codex updates pending task and records success activity`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledCodexConfig()
        val workspaceDir = Files.createTempDirectory("codex-success").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val envSlot = slot<Map<String, String>>()
        coEvery { codexConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { codexCliService.resolveExecutable(null) } returns Result.success("/bin/codex")
        coEvery { codexCliService.validateExecutable("/bin/codex") } returns Result.success(Unit)
        coEvery { codexConfigurationRepository.findApiKey(config.id) } returns null
        coEvery { codexCliService.launchInTerminal(eq("/bin/codex"), eq(workspaceDir.absolutePath), capture(envSlot)) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId, updateTaskStatus = true))
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals(TaskStatus.IN_PROGRESS, response?.task?.status)
        assertEquals(ActivityType.CODEX_LAUNCHED, response?.activity?.type)
        assertEquals(ActivityStatus.SUCCESS, response?.activity?.status)
        assertTrue(File(workspaceDir, "TASK_CONTEXT.md").exists())
        assertEquals(project.name, envSlot.captured["AITASK_PROJECT_NAME"])
        assertEquals(workspaceDir.absolutePath, envSlot.captured["AITASK_REPOSITORY_PATH"])
        coVerify { taskRepository.update(match { it.status == TaskStatus.IN_PROGRESS }) }
    }

    @Test
    fun `includes OPENAI_API_KEY in environment when stored`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val config = enabledCodexConfig()
        val workspaceDir = Files.createTempDirectory("codex-env-key").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = minimalTask(taskId, projectId, workspacePath = workspaceDir.absolutePath)
        val project = minimalProject(projectId)
        val envSlot = slot<Map<String, String>>()
        coEvery { codexConfigurationRepository.find() } returns config
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { codexCliService.resolveExecutable(null) } returns Result.success("/bin/codex")
        coEvery { codexCliService.validateExecutable("/bin/codex") } returns Result.success(Unit)
        coEvery { codexConfigurationRepository.findApiKey(config.id) } returns "secret-key"
        coEvery { codexCliService.launchInTerminal(any(), any(), capture(envSlot)) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        val result = useCase(LaunchCodexRequest(taskId = taskId, updateTaskStatus = false))
        assertTrue(result.isSuccess)
        assertEquals("secret-key", envSlot.captured["OPENAI_API_KEY"])
    }

    private fun enabledCodexConfig(): CodexConfiguration {
        val id = UUID.randomUUID()
        return CodexConfiguration(
            id = id,
            isEnabled = true,
            cliPath = "",
            hasApiKey = false,
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
