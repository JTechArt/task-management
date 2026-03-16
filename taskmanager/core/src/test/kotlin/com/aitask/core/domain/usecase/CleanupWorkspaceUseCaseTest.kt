package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityStatus
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.RetentionPolicy
import com.aitask.core.domain.service.WorkspaceService
import com.aitask.core.infrastructure.workspace.WorkspaceCleanupException
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CleanupWorkspaceUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var workspaceService: WorkspaceService
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: CleanupWorkspaceUseCase

    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        workspaceService = mockk()
        activityRepository = mockk()
        useCase = CleanupWorkspaceUseCase(
            taskRepository,
            projectRepository,
            workspaceService,
            activityRepository
        )
    }

    private fun createCompletedTaskWithWorkspace(
        taskId: UUID = UUID.randomUUID(),
        projectId: UUID = UUID.randomUUID(),
        workspacePath: String = "/workspaces/proj/task-123",
        workspaceCleanedAt: Instant? = null
    ) = Task(
        id = taskId,
        title = "Test Task",
        description = null,
        taskType = TaskType.FEATURE,
        status = TaskStatus.COMPLETED,
        projectId = projectId,
        workspacePath = workspacePath,
        workspaceCleanedAt = workspaceCleanedAt,
        branchName = "task-123",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = Instant.now()
    )

    private fun createProject(projectId: UUID = UUID.randomUUID()) = Project(
        id = projectId,
        name = "Test Project",
        description = null,
        workspacePath = "/workspaces/proj",
        branchTemplate = "task-{taskId}",
        retentionPolicy = RetentionPolicy.KEEP_ALL,
        tags = emptyList(),
        team = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        archivedAt = null
    )

    @Test
    fun `should cleanup workspace when task is completed and has workspace`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspaces/proj/task-123"
        val task = createCompletedTaskWithWorkspace(taskId, projectId, workspacePath)
        val project = createProject(projectId)

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { workspaceService.cleanupWorkspace(workspacePath, RetentionPolicy.DELETE_ON_COMPLETION) } returns Result.success(Unit)
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(taskId)

        assertTrue(result.isSuccess)
        coVerify { workspaceService.cleanupWorkspace(workspacePath, RetentionPolicy.DELETE_ON_COMPLETION) }
        coVerify { taskRepository.update(match { it.workspaceCleanedAt != null }) }
        coVerify { activityRepository.create(match { it.type == ActivityType.WORKSPACE_CLEANUP && it.status == ActivityStatus.SUCCESS }) }
    }

    @Test
    fun `should fail when task not found`() = runTest {
        val taskId = UUID.randomUUID()
        coEvery { taskRepository.findById(taskId) } returns null

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
        coVerify(exactly = 0) { workspaceService.cleanupWorkspace(any(), any()) }
        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `should fail when task is not completed`() = runTest {
        val taskId = UUID.randomUUID()
        val task = createCompletedTaskWithWorkspace(taskId).copy(status = TaskStatus.IN_PROGRESS)
        coEvery { taskRepository.findById(taskId) } returns task

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WorkspaceCleanupException)
        coVerify(exactly = 0) { workspaceService.cleanupWorkspace(any(), any()) }
    }

    @Test
    fun `should fail when task has no workspace path`() = runTest {
        val taskId = UUID.randomUUID()
        val task = createCompletedTaskWithWorkspace(taskId).copy(workspacePath = null)
        coEvery { taskRepository.findById(taskId) } returns task

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WorkspaceCleanupException)
        coVerify(exactly = 0) { workspaceService.cleanupWorkspace(any(), any()) }
    }

    @Test
    fun `should fail when workspace already cleaned`() = runTest {
        val taskId = UUID.randomUUID()
        val task = createCompletedTaskWithWorkspace(taskId, workspaceCleanedAt = Instant.now())
        coEvery { taskRepository.findById(taskId) } returns task

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WorkspaceCleanupException)
        coVerify(exactly = 0) { workspaceService.cleanupWorkspace(any(), any()) }
    }

    @Test
    fun `should fail when project not found`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val task = createCompletedTaskWithWorkspace(taskId, projectId)
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns null

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
        coVerify(exactly = 0) { workspaceService.cleanupWorkspace(any(), any()) }
    }

    @Test
    fun `should record activity and not update task when cleanup fails`() = runTest {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val workspacePath = "/workspaces/proj/task-123"
        val task = createCompletedTaskWithWorkspace(taskId, projectId, workspacePath)
        val project = createProject(projectId)
        val error = WorkspaceCleanupException("Permission denied")

        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { workspaceService.cleanupWorkspace(workspacePath, RetentionPolicy.DELETE_ON_COMPLETION) } returns Result.failure(error)
        coEvery { activityRepository.create(any()) } answers { firstArg() }

        val result = useCase(taskId)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { taskRepository.update(any()) }
        coVerify { activityRepository.create(match { it.type == ActivityType.WORKSPACE_CLEANUP && it.status == ActivityStatus.FAILED }) }
    }
}
