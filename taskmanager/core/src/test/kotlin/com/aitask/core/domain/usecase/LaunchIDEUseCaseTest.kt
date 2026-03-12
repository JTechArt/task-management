package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.IDEService
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LaunchIDEUseCaseTest {
    
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var ideService: IDEService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: LaunchIDEUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        repositoryRepository = mockk()
        ideService = mockk()
        activityRepository = mockk()
        useCase = LaunchIDEUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
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
        coEvery { ideService.launchIDE(any(), any(), any()) } returns
            Result.failure(IDENotFoundException(IDEType.CURSOR))

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IDENotFoundException)

        coVerify(exactly = 0) { taskRepository.update(any()) }
        coVerify(exactly = 0) { activityRepository.create(any()) }
    }
}

