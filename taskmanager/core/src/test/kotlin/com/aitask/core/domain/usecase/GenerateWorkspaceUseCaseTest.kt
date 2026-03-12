package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.WorkspaceService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GenerateWorkspaceUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var workspaceService: WorkspaceService
    private lateinit var useCase: GenerateWorkspaceUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        repositoryRepository = mockk()
        workspaceService = mockk()
        useCase = GenerateWorkspaceUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            workspaceService
        )
    }
    
    private fun createTask(
        id: UUID = UUID.randomUUID(),
        projectId: UUID = UUID.randomUUID(),
        status: TaskStatus = TaskStatus.PENDING,
        archived: Boolean = false
    ) = Task(
        id = id,
        title = "Test Task",
        description = "Test description",
        taskType = TaskType.FEATURE,
        status = if (archived) TaskStatus.ARCHIVED else status,
        projectId = projectId,
        workspacePath = null,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )
    
    private fun createProject(
        id: UUID = UUID.randomUUID(),
        archived: Boolean = false
    ) = Project(
        id = id,
        name = "Test Project",
        description = null,
        workspacePath = "/workspace",
        branchTemplate = "task-{taskId}",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        archivedAt = if (archived) Instant.now() else null
    )
    
    private fun createRepository(
        id: UUID = UUID.randomUUID(),
        projectId: UUID,
        isPrimary: Boolean = true
    ) = Repository(
        id = id,
        projectId = projectId,
        name = "test-repo",
        cloneUrl = "https://github.com/test/repo.git",
        provider = GitProvider.GITHUB,
        authType = AuthType.HTTPS,
        preferredIDEs = listOf(IDEType.CURSOR),
        isPrimary = isPrimary,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    
    private fun createWorkspace(
        taskId: UUID,
        projectId: UUID,
        status: WorkspaceStatus = WorkspaceStatus.COMPLETED
    ) = Workspace(
        taskId = taskId,
        projectId = projectId,
        path = "/workspace/test-project/task-12345678",
        repositories = listOf(
            WorkspaceRepository(
                repositoryId = UUID.randomUUID(),
                localPath = "./test-repo",
                branchName = null,
                cloneStatus = CloneStatus.COMPLETED
            )
        ),
        appliedRules = emptyList(),
        status = status,
        createdAt = Instant.now()
    )
    
    @Test
    fun `should generate workspace successfully`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId)
        val project = createProject(id = projectId)
        val repository = createRepository(projectId = projectId)
        val workspace = createWorkspace(taskId, projectId)
        
        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )
        
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findPrimaryByProject(projectId) } returns repository
        coEvery { workspaceService.createWorkspace(task, project, listOf(repository)) } returns Result.success(workspace)
        coEvery { workspaceService.prepareWorkspace(workspace, listOf(repository), any()) } returns Result.success(workspace)
        coEvery { taskRepository.update(any()) } returns mockk()
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val generatedWorkspace = result.getOrThrow()
        assertEquals(taskId, generatedWorkspace.taskId)
        assertEquals(projectId, generatedWorkspace.projectId)
        assertTrue(generatedWorkspace.isCompleted)
        
        coVerify { taskRepository.update(match { it.workspacePath == workspace.path && it.status == TaskStatus.IN_PROGRESS }) }
    }
    
    @Test
    fun `should fail when task not found`() = runTest {
        // Given
        val request = CreateWorkspaceRequest(
            taskId = UUID.randomUUID(),
            projectId = UUID.randomUUID()
        )

        coEvery { taskRepository.findById(request.taskId) } returns null

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
    }

    @Test
    fun `should fail when project not found`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId)

        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns null

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }

    @Test
    fun `should fail when project is archived`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId)
        val project = createProject(id = projectId, archived = true)

        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedProjectException)
    }

    @Test
    fun `should fail when task is archived`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId, archived = true)
        val project = createProject(id = projectId)

        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedTaskException)
    }

    @Test
    fun `should not update task when workspace generation fails`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId)
        val project = createProject(id = projectId)
        val repository = createRepository(projectId = projectId)
        val failedWorkspace = createWorkspace(taskId, projectId, WorkspaceStatus.FAILED)
            .copy(errorMessage = "Clone failed")

        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findPrimaryByProject(projectId) } returns repository
        coEvery { workspaceService.createWorkspace(task, project, listOf(repository)) } returns Result.success(failedWorkspace)
        coEvery { workspaceService.prepareWorkspace(failedWorkspace, listOf(repository), any()) } returns Result.success(failedWorkspace)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WorkspaceGenerationException)

        // Verify task was NOT updated
        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `should use all repositories when no primary repository exists`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createTask(id = taskId, projectId = projectId)
        val project = createProject(id = projectId)
        val repo1 = createRepository(projectId = projectId, isPrimary = false)
        val repo2 = createRepository(projectId = projectId, isPrimary = false)
        val workspace = createWorkspace(taskId, projectId)

        val request = CreateWorkspaceRequest(
            taskId = taskId,
            projectId = projectId
        )

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findPrimaryByProject(projectId) } returns null
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repo1, repo2)
        coEvery { workspaceService.createWorkspace(task, project, listOf(repo1, repo2)) } returns Result.success(workspace)
        coEvery { workspaceService.prepareWorkspace(workspace, listOf(repo1, repo2), any()) } returns Result.success(workspace)
        coEvery { taskRepository.update(any()) } returns mockk()

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        coVerify { repositoryRepository.findByProject(projectId) }
    }
}

