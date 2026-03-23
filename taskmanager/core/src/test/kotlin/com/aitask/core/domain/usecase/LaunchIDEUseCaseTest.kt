package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.PreRunScriptRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.PreRunScriptService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

class LaunchIDEUseCaseTest {
    
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var preRunScriptRepository: PreRunScriptRepository
    private lateinit var preRunScriptService: PreRunScriptService
    private lateinit var bmadConfigurationResolver: BmadConfigurationResolver
    private lateinit var ideService: IDEService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: LaunchIDEUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        repositoryRepository = mockk()
        preRunScriptRepository = mockk()
        preRunScriptService = mockk()
        bmadConfigurationResolver = BmadConfigurationResolver()
        ideService = mockk()
        activityRepository = mockk()
        useCase = LaunchIDEUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            preRunScriptRepository,
            preRunScriptService,
            bmadConfigurationResolver,
            ideService,
            activityRepository
        )
    }
    
    @Test
    fun `should launch IDE successfully and update task status`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test-project/task-123"
        
        val task = Task(
            id = taskId,
            title = "Test Task",
            description = "Test Description",
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = workspacePath,
            branchName = "feature/test",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val repository = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = LaunchIDERequest(
            taskId = taskId,
            ideType = IDEType.CURSOR,
            updateTaskStatus = true
        )
        
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repository)
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { ideService.launchIDE(any(), any(), any()) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(TaskStatus.IN_PROGRESS, response?.task?.status)
        assertEquals(ActivityType.IDE_LAUNCHED, response?.activity?.type)
        
        coVerify { ideService.launchIDE(IDEType.CURSOR, workspacePath, any()) }
        coVerify { taskRepository.update(match { it.status == TaskStatus.IN_PROGRESS }) }
        coVerify { activityRepository.create(match { it.type == ActivityType.IDE_LAUNCHED }) }
    }
    
    @Test
    fun `should fail when task not found`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val request = LaunchIDERequest(taskId, IDEType.CURSOR)
        
        coEvery { taskRepository.findById(taskId) } returns null
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
    }
    
    @Test
    fun `should fail when task has no workspace`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val task = Task(
            id = taskId,
            title = "Test Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = UUID.randomUUID(),
            workspacePath = null, // No workspace
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )

        val request = LaunchIDERequest(taskId, IDEType.CURSOR)

        coEvery { taskRepository.findById(taskId) } returns task

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(result.exceptionOrNull()?.message?.contains("workspace") == true)
    }

    @Test
    fun `should fail when project not found`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = Task(
            id = taskId,
            title = "Test Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = "/workspace/test",
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )

        val request = LaunchIDERequest(taskId, IDEType.CURSOR)

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns null

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }

    @Test
    fun `should not update task status when updateTaskStatus is false`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspace/test-project/task-123"

        val task = Task(
            id = taskId,
            title = "Test Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = workspacePath,
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )

        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val request = LaunchIDERequest(
            taskId = taskId,
            ideType = IDEType.CURSOR,
            updateTaskStatus = false
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { ideService.launchIDE(any(), any(), any()) } returns Result.success(Unit)
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals(TaskStatus.PENDING, response?.task?.status)

        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `should fail when IDE launch fails`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()

        val task = Task(
            id = taskId,
            title = "Test Task",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = "/workspace/test",
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )

        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val request = LaunchIDERequest(taskId, IDEType.CURSOR)

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { ideService.launchIDE(any(), any(), any()) } returns
            Result.failure(IDENotFoundException(IDEType.CURSOR))
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IDENotFoundException)

        coVerify(exactly = 0) { taskRepository.update(any()) }
        coVerify(exactly = 1) { activityRepository.create(any()) }
    }

    @Test
    fun `should execute pre-run scripts before launching IDE`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("launch-ide-test").toFile()
        val task = createTask(taskId, projectId, workspaceDir.absolutePath)
        val project = createProject(projectId)
        val scripts = listOf(
            PreRunScript(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = "Check Node",
                type = PreRunScriptType.NODE_VERSION,
                requiredValue = "20",
                executionOrder = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        workspaceDir.resolve("workspace-metadata.json").writeText(
            """
            {
              "repositories": []
            }
            """.trimIndent()
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns scripts
        coEvery { preRunScriptService.executeScripts(scripts, task.workspacePath!!) } returns Result.success(emptyList())
        coEvery { ideService.launchIDE(any(), any(), any()) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(LaunchIDERequest(taskId, IDEType.CURSOR))

        assertTrue(result.isSuccess)
        coVerifyOrder {
            preRunScriptService.executeScripts(scripts, task.workspacePath!!)
            ideService.launchIDE(IDEType.CURSOR, task.workspacePath!!, any())
        }
        coVerify { activityRepository.create(match { it.type == ActivityType.PRE_RUN_SUCCESS }) }
    }

    @Test
    fun `should fail before IDE launch when pre-run scripts fail`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("launch-ide-test-failure").toFile()
        val task = createTask(taskId, projectId, workspaceDir.absolutePath)
        val project = createProject(projectId)
        val scripts = listOf(
            PreRunScript(
                id = UUID.randomUUID(),
                projectId = projectId,
                name = "Check Env",
                type = PreRunScriptType.ENVIRONMENT_VARIABLE,
                requiredValue = "JAVA_HOME",
                executionOrder = 1,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        workspaceDir.resolve("workspace-metadata.json").writeText(
            """
            {
              "repositories": []
            }
            """.trimIndent()
        )
        val failure = PreRunScriptExecutionException(
            PreRunExecutionResult(
                script = scripts.first(),
                succeeded = false,
                exitCode = 1,
                stderr = "Missing required environment variable: JAVA_HOME",
                resolvedCommand = "env check"
            )
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns scripts
        coEvery { preRunScriptService.executeScripts(scripts, task.workspacePath!!) } returns Result.failure(failure)
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(LaunchIDERequest(taskId, IDEType.CURSOR))

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
        coVerify(exactly = 0) { ideService.launchIDE(any(), any(), any()) }
        coVerify { activityRepository.create(match { it.type == ActivityType.PRE_RUN_FAILED }) }
    }

    @Test
    fun `should execute project scripts before selected repository scripts only`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val repositoryA = UUID.randomUUID()
        val repositoryB = UUID.randomUUID()
        val repositoryC = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("launch-ide-order").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText(
            """
            {
              "repositories": [
                { "repositoryId": "$repositoryB" },
                { "repositoryId": "$repositoryA" }
              ]
            }
            """.trimIndent()
        )
        val task = createTask(taskId, projectId, workspaceDir.absolutePath)
        val project = createProject(projectId)
        val orderedScripts = listOf(
            createScript(projectId, "Project First", 1, repositoryId = null),
            createScript(projectId, "Repo A", 1, repositoryId = repositoryA),
            createScript(projectId, "Repo B", 1, repositoryId = repositoryB),
            createScript(projectId, "Repo C", 1, repositoryId = repositoryC)
        )
        val capturedScripts = slot<List<PreRunScript>>()

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns orderedScripts
        coEvery { preRunScriptService.executeScripts(capture(capturedScripts), task.workspacePath!!) } returns Result.success(emptyList())
        coEvery { ideService.launchIDE(any(), any(), any()) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(LaunchIDERequest(taskId, IDEType.CURSOR))

        assertTrue(result.isSuccess)
        assertEquals(
            listOf("Project First", "Repo B", "Repo A"),
            capturedScripts.captured.map { it.name }
        )
    }

    @Test
    fun `should include active bmad tools in task context when project uses bmad`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspaceDir = Files.createTempDirectory("launch-ide-bmad-context").toFile()
        workspaceDir.resolve("workspace-metadata.json").writeText("""{ "repositories": [] }""")
        val task = createTask(taskId, projectId, workspaceDir.absolutePath).copy(
            bmadToolOverrideIds = listOf("agent:dev", "task:execute-checklist")
        )
        val project = createProject(projectId).copy(
            methodology = Methodology.BMAD,
            bmadToolIds = BmadToolCatalog.defaultToolIds
        )
        val taskContextSlot = slot<TaskContext>()

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { preRunScriptRepository.findByProject(projectId) } returns emptyList()
        coEvery { ideService.launchIDE(any(), any(), capture(taskContextSlot)) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(LaunchIDERequest(taskId, IDEType.CURSOR))

        assertTrue(result.isSuccess)
        assertEquals(listOf("Full Stack Developer", "Execute Checklist"), taskContextSlot.captured.activeBmadTools)
    }

    private fun createTask(taskId: UUID, projectId: UUID, workspacePath: String) = Task(
        id = taskId,
        title = "Test Task",
        description = null,
        taskType = TaskType.FEATURE,
        status = TaskStatus.PENDING,
        projectId = projectId,
        workspacePath = workspacePath,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )

    private fun createProject(projectId: UUID) = Project(
        id = projectId,
        name = "Test Project",
        description = null,
        workspacePath = "/workspace",
        branchTemplate = "task-{taskId}",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createScript(
        projectId: UUID,
        name: String,
        executionOrder: Int,
        repositoryId: UUID?
    ) = PreRunScript(
        id = UUID.randomUUID(),
        projectId = projectId,
        repositoryId = repositoryId,
        name = name,
        type = PreRunScriptType.INLINE_COMMAND,
        inlineScript = "echo ok",
        executionOrder = executionOrder,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
