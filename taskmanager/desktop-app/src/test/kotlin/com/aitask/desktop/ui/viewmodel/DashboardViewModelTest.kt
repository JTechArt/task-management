package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.Activity
import com.aitask.core.domain.model.ActivityType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val taskRepository = mockk<TaskRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val activityRepository = mockk<ActivityRepository>()
    private val ruleRepository = mockk<RuleRepository>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: DashboardViewModel

    @BeforeEach
    fun setUp() {
        viewModel = DashboardViewModel(
            taskRepository = taskRepository,
            projectRepository = projectRepository,
            activityRepository = activityRepository,
            ruleRepository = ruleRepository,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `loadDashboard populates metrics from repositories`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        val projects = listOf(
            createProject(id = projectId, name = "Test Project")
        )
        val tasks = listOf(
            createTask(projectId = projectId, status = TaskStatus.IN_PROGRESS),
            createTask(projectId = projectId, status = TaskStatus.PENDING)
        )
        coEvery { projectRepository.findAllActive() } returns projects
        coEvery { taskRepository.findAll() } returns tasks
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns tasks.filter { it.status == TaskStatus.IN_PROGRESS }
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns tasks.filter { it.status == TaskStatus.PENDING }
        coEvery { activityRepository.findRecent(any()) } returns emptyList()
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals(2, state.metrics.openTasksCount)
        assertEquals(2, state.metrics.totalTasksCount)
        assertEquals(1, state.metrics.projectsCount)
        assertEquals(2, state.recentTasks.size)
    }

    @Test
    fun `loadDashboard shows empty state when no data`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns emptyList()
        coEvery { activityRepository.findRecent(any()) } returns emptyList()
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals(0, state.metrics.openTasksCount)
        assertEquals(0, state.metrics.totalTasksCount)
        assertEquals(0, state.metrics.projectsCount)
        assertTrue(state.recentTasks.isEmpty())
    }

    @Test
    fun `findProjectName returns project name when project exists`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        val projects = listOf(createProject(id = projectId, name = "My Project"))
        coEvery { projectRepository.findAllActive() } returns projects
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns emptyList()
        coEvery { activityRepository.findRecent(any()) } returns emptyList()
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val name = viewModel.findProjectName(projectId)
        assertEquals("My Project", name)
    }

    @Test
    fun `findProjectName returns Unknown project when project not found`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns emptyList()
        coEvery { activityRepository.findRecent(any()) } returns emptyList()
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val name = viewModel.findProjectName(UUID.randomUUID())
        assertEquals("Unknown project", name)
    }

    @Test
    fun `loadDashboard sets error and stops loading when repository throws`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } throws RuntimeException("Database connection failed")
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns emptyList()
        coEvery { activityRepository.findRecent(any()) } returns emptyList()
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("Database connection failed", state.error)
    }

    @Test
    fun `loadDashboard sets error and clears loading when repository throws`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } throws RuntimeException("Database connection failed")
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        val state = viewModel.uiState
        assertFalse(state.isLoading)
        assertEquals("Database connection failed", state.error)
    }

    @Test
    fun `loadDashboard includes recent activity`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        val activities = listOf(
            Activity(
                id = UUID.randomUUID(),
                type = ActivityType.IDE_LAUNCHED,
                entityType = "task",
                entityId = UUID.randomUUID(),
                description = "IDE launched",
                metadata = emptyMap(),
                createdAt = Instant.now(),
                status = com.aitask.core.domain.model.ActivityStatus.SUCCESS
            )
        )
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.IN_PROGRESS) } returns emptyList()
        coEvery { taskRepository.findByStatus(TaskStatus.PENDING) } returns emptyList()
        coEvery { activityRepository.findRecent(10) } returns activities
        coEvery { ruleRepository.findAll() } returns emptyList()
        viewModel.loadDashboard(databaseConnected = true)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.recentActivity.size)
        assertEquals("IDE launched", viewModel.uiState.recentActivity.first().description)
    }

    private fun createProject(id: UUID = UUID.randomUUID(), name: String = "Project"): Project {
        return Project(
            id = id,
            name = name,
            description = null,
            workspacePath = "/tmp/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            archivedAt = null
        )
    }

    private fun createTask(
        projectId: UUID,
        status: TaskStatus = TaskStatus.PENDING
    ): Task {
        return Task(
            id = UUID.randomUUID(),
            title = "Test Task",
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
    }
}
