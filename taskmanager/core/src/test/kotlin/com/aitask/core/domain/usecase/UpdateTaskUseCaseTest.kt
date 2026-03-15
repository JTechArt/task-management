package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.model.UpdateTaskRequest
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class UpdateTaskUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var useCase: UpdateTaskUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        useCase = UpdateTaskUseCase(taskRepository)
    }
    
    private fun createTask(
        id: UUID = UUID.randomUUID(),
        status: TaskStatus = TaskStatus.PENDING
    ) = Task(
        id = id,
        title = "Original Title",
        description = "Original description",
        taskType = TaskType.FEATURE,
        status = status,
        projectId = UUID.randomUUID(),
        workspacePath = null,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )
    
    @Test
    fun `should update task when valid`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val existingTask = createTask(id = taskId)
        val request = UpdateTaskRequest(
            title = "Updated Title",
            description = "Updated description",
            taskType = TaskType.BUG_FIX,
            status = TaskStatus.IN_PROGRESS
        )
        
        coEvery { taskRepository.findById(taskId) } returns existingTask
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(taskId, request)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedTask = result.getOrThrow()
        assertEquals("Updated Title", updatedTask.title)
        assertEquals("Updated description", updatedTask.description)
        assertEquals(TaskType.BUG_FIX, updatedTask.taskType)
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.status)
        
        coVerify { taskRepository.update(any()) }
    }
    
    @Test
    fun `should fail when task not found`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val request = UpdateTaskRequest(title = "Updated Title")
        
        coEvery { taskRepository.findById(taskId) } returns null
        
        // When
        val result = useCase(taskId, request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
        
        coVerify(exactly = 0) { taskRepository.update(any()) }
    }
    
    @Test
    fun `should fail when task is archived`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val archivedTask = createTask(id = taskId, status = TaskStatus.ARCHIVED)
        val request = UpdateTaskRequest(title = "Updated Title")
        
        coEvery { taskRepository.findById(taskId) } returns archivedTask
        
        // When
        val result = useCase(taskId, request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ArchivedTaskException)
        
        coVerify(exactly = 0) { taskRepository.update(any()) }
    }
    
    @Test
    fun `should update only specified fields`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val existingTask = createTask(id = taskId)
        val request = UpdateTaskRequest(title = "New Title")
        
        coEvery { taskRepository.findById(taskId) } returns existingTask
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(taskId, request)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedTask = result.getOrThrow()
        assertEquals("New Title", updatedTask.title)
        assertEquals(existingTask.description, updatedTask.description)
        assertEquals(existingTask.taskType, updatedTask.taskType)
        assertEquals(existingTask.status, updatedTask.status)
    }
    
    @Test
    fun `should set completion timestamp when status changes to COMPLETED`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val existingTask = createTask(id = taskId, status = TaskStatus.IN_PROGRESS)
        val request = UpdateTaskRequest(status = TaskStatus.COMPLETED)
        
        coEvery { taskRepository.findById(taskId) } returns existingTask
        coEvery { taskRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(taskId, request)
        
        // Then
        assertTrue(result.isSuccess)
        val updatedTask = result.getOrThrow()
        assertEquals(TaskStatus.COMPLETED, updatedTask.status)
        assertNotNull(updatedTask.completedAt)
    }
}

