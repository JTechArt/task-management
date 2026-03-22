package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteTaskUseCaseTest {
    private lateinit var taskRepository: TaskRepository
    private lateinit var activityRepository: ActivityRepository
    private lateinit var useCase: DeleteTaskUseCase
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        activityRepository = mockk(relaxed = true)
        coEvery { activityRepository.create(any()) } answers { firstArg() }
        useCase = DeleteTaskUseCase(taskRepository, activityRepository)
    }
    
    private fun createTask(id: UUID = UUID.randomUUID()) = Task(
        id = id,
        title = "Test Task",
        description = "Test description",
        taskType = TaskType.FEATURE,
        status = TaskStatus.PENDING,
        projectId = UUID.randomUUID(),
        workspacePath = null,
        branchName = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        completedAt = null
    )
    
    @Test
    fun `should delete task when exists`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val task = createTask(id = taskId)
        coEvery { taskRepository.findById(taskId) } returns task
        coEvery { taskRepository.delete(taskId) } just Runs
        // When
        val result = useCase(taskId)
        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.findById(taskId) }
        coVerify { activityRepository.create(match { it.type == ActivityType.TASK_DELETED }) }
        coVerify { taskRepository.delete(taskId) }
    }
    
    @Test
    fun `should fail when task not found`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        
        coEvery { taskRepository.findById(taskId) } returns null
        
        // When
        val result = useCase(taskId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TaskNotFoundException)
        
        coVerify { taskRepository.findById(taskId) }
        coVerify(exactly = 0) { taskRepository.delete(any()) }
    }
    
    @Test
    fun `should delete archived task`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val archivedTask = createTask(id = taskId).copy(status = TaskStatus.ARCHIVED)
        
        coEvery { taskRepository.findById(taskId) } returns archivedTask
        coEvery { taskRepository.delete(taskId) } just Runs
        
        // When
        val result = useCase(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { taskRepository.delete(taskId) }
    }
    
    @Test
    fun `should delete completed task`() = runTest {
        // Given
        val taskId = UUID.randomUUID()
        val completedTask = createTask(id = taskId).copy(
            status = TaskStatus.COMPLETED,
            completedAt = Instant.now()
        )
        
        coEvery { taskRepository.findById(taskId) } returns completedTask
        coEvery { taskRepository.delete(taskId) } just Runs
        
        // When
        val result = useCase(taskId)
        
        // Then
        assertTrue(result.isSuccess)
        
        coVerify { taskRepository.delete(taskId) }
    }
}

