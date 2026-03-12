package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetTasksUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var useCase: GetTasksUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        useCase = GetTasksUseCase(taskRepository)
    }
    
    private fun createTask(
        title: String,
        projectId: UUID,
        status: TaskStatus = TaskStatus.PENDING
    ) = Task(
        id = UUID.randomUUID(),
        title = title,
        description = null,
        taskType = TaskType.FEATURE,
        status = status,
        projectId = projectId,
        workspacePath = null,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )
    
    @Test
    fun `should get all tasks when no filters`() = runTest {
        // Given
        val tasks = listOf(
            createTask("Task 1", UUID.randomUUID()),
            createTask("Task 2", UUID.randomUUID())
        )
        
        coEvery { taskRepository.findAll() } returns tasks
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        
        coVerify { taskRepository.findAll() }
    }
    
    @Test
    fun `should filter by project`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val tasks = listOf(
            createTask("Task 1", projectId),
            createTask("Task 2", projectId)
        )
        
        coEvery { taskRepository.findByProject(projectId) } returns tasks
        
        // When
        val result = useCase(projectId = projectId)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        
        coVerify { taskRepository.findByProject(projectId) }
    }
    
    @Test
    fun `should filter by status`() = runTest {
        // Given
        val tasks = listOf(
            createTask("Task 1", UUID.randomUUID(), TaskStatus.IN_PROGRESS),
            createTask("Task 2", UUID.randomUUID(), TaskStatus.IN_PROGRESS)
        )
        
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns tasks
        
        // When
        val result = useCase(status = TaskStatus.IN_PROGRESS)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        
        coVerify { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) }
    }
    
    @Test
    fun `should filter by project and status`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val tasks = listOf(
            createTask("Task 1", projectId, TaskStatus.COMPLETED)
        )
        
        coEvery { taskRepository.findByProjectAndStatus(projectId, TaskStatus.COMPLETED) } returns tasks
        
        // When
        val result = useCase(projectId = projectId, status = TaskStatus.COMPLETED)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        
        coVerify { taskRepository.findByProjectAndStatus(projectId, TaskStatus.COMPLETED) }
    }
}

