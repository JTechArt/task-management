package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.validation.TaskValidator
import com.aitask.core.domain.validation.ValidationResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CreateTaskUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var activityRepository: ActivityRepository
    private lateinit var taskValidator: TaskValidator
    private lateinit var useCase: CreateTaskUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        activityRepository = mockk()
        taskValidator = mockk()
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        useCase = CreateTaskUseCase(
            taskRepository,
            projectRepository,
            activityRepository,
            taskValidator
        )
    }
    
    @Test
    fun `should create task when valid`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val request = CreateTaskRequest(
            title = "Implement feature",
            description = "Add new feature",
            taskType = TaskType.FEATURE,
            projectId = projectId,
            methodologyOverride = Methodology.BMAD
        )
        
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            methodology = Methodology.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        every { taskValidator.validate(request) } returns ValidationResult.success()
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { taskRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val task = result.getOrThrow()
        assertEquals("Implement feature", task.title)
        assertEquals(TaskType.FEATURE, task.taskType)
        assertEquals(TaskStatus.PENDING, task.status)
        assertEquals(projectId, task.projectId)
        assertEquals(Methodology.BMAD, task.methodologyOverride)
        
        coVerify { taskRepository.create(any()) }
    }
    
    @Test
    fun `should fail when validation fails`() = runTest {
        // Given
        val request = CreateTaskRequest(
            title = "",
            taskType = TaskType.FEATURE,
            projectId = UUID.randomUUID()
        )
        
        every { taskValidator.validate(request) } returns ValidationResult.failure(
            listOf(com.aitask.core.domain.validation.ValidationError("title", "Title is required", "REQUIRED"))
        )
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        
        coVerify(exactly = 0) { taskRepository.create(any()) }
    }
    
    @Test
    fun `should fail when project not found`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val request = CreateTaskRequest(
            title = "Implement feature",
            taskType = TaskType.FEATURE,
            projectId = projectId
        )
        
        every { taskValidator.validate(request) } returns ValidationResult.success()
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
        val projectId = UUID.randomUUID()
        val request = CreateTaskRequest(
            title = "Implement feature",
            taskType = TaskType.FEATURE,
            projectId = projectId
        )
        
        val archivedProject = Project(
            id = projectId,
            name = "Archived Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            methodology = Methodology.NONE,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = Instant.now()
        )
        
        every { taskValidator.validate(request) } returns ValidationResult.success()
        coEvery { projectRepository.findById(projectId) } returns archivedProject
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedProjectException)
    }
}
