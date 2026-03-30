package com.aitask.desktop.ui.viewmodel

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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {

    private val activityRepository = mockk<ActivityRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ActivityViewModel

    @BeforeEach
    fun setUp() {
        viewModel = ActivityViewModel(
            activityRepository = activityRepository,
            projectRepository = projectRepository,
            taskRepository = taskRepository,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
    }

    @Test
    fun `loadActivities populates activities from repository`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        val activities = listOf(
            createActivity(type = ActivityType.TASK_CREATED, projectId = projectId),
            createActivity(type = ActivityType.IDE_LAUNCHED, projectId = projectId)
        )
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery {
            activityRepository.findFiltered(
                projectId = null,
                taskId = null,
                type = null,
                status = null,
                createdAfterInclusive = null,
                createdBeforeExclusive = null,
                limit = 100
            )
        } returns activities
        viewModel.loadActivities()
        advanceUntilIdle()
        val state = viewModel.uiState
        assertEquals(2, state.activities.size)
        assertEquals(ActivityType.TASK_CREATED, state.activities[0].type)
        assertEquals(ActivityType.IDE_LAUNCHED, state.activities[1].type)
        assertNull(state.error)
    }

    @Test
    fun `loadActivities with project filter passes projectId to findFiltered`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        val activities = listOf(createActivity(projectId = projectId))
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findByProject(projectId) } returns emptyList()
        coEvery {
            activityRepository.findFiltered(
                projectId = projectId,
                taskId = null,
                type = null,
                status = null,
                createdAfterInclusive = null,
                createdBeforeExclusive = null,
                limit = 100
            )
        } returns activities
        viewModel.setProjectFilter(projectId)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.activities.size)
        assertEquals(projectId, viewModel.uiState.filters.projectId)
    }

    @Test
    fun `setTypeFilter updates filter and reloads`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery {
            activityRepository.findFiltered(
                projectId = null,
                taskId = null,
                type = ActivityType.IDE_LAUNCHED,
                status = null,
                createdAfterInclusive = null,
                createdBeforeExclusive = null,
                limit = 100
            )
        } returns emptyList()
        viewModel.setTypeFilter(ActivityType.IDE_LAUNCHED)
        advanceUntilIdle()
        assertEquals(ActivityType.IDE_LAUNCHED, viewModel.uiState.filters.type)
    }

    @Test
    fun `clearFilters resets all filters`() = runTest(testDispatcher) {
        val projectId = UUID.randomUUID()
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findByProject(projectId) } returns emptyList()
        coEvery {
            activityRepository.findFiltered(
                projectId = projectId,
                taskId = null,
                type = null,
                status = null,
                createdAfterInclusive = null,
                createdBeforeExclusive = null,
                limit = 100
            )
        } returns emptyList()
        viewModel.setProjectFilter(projectId)
        advanceUntilIdle()
        coEvery {
            activityRepository.findFiltered(
                projectId = null,
                taskId = null,
                type = null,
                status = null,
                createdAfterInclusive = null,
                createdBeforeExclusive = null,
                limit = 100
            )
        } returns emptyList()
        viewModel.clearFilters()
        advanceUntilIdle()
        assertNull(viewModel.uiState.filters.projectId)
        assertNull(viewModel.uiState.filters.taskId)
        assertNull(viewModel.uiState.filters.type)
        assertNull(viewModel.uiState.filters.status)
        assertEquals("", viewModel.uiState.filters.dateFrom)
        assertEquals("", viewModel.uiState.filters.searchQuery)
    }

    @Test
    fun `loadActivities sets error when repository throws`() = runTest(testDispatcher) {
        coEvery { projectRepository.findAllActive() } returns emptyList()
        coEvery { taskRepository.findAll() } returns emptyList()
        coEvery {
            activityRepository.findFiltered(any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("DB error")
        viewModel.loadActivities()
        advanceUntilIdle()
        assertEquals("DB error", viewModel.uiState.error)
    }

    private fun createActivity(
        type: ActivityType = ActivityType.TASK_CREATED,
        projectId: UUID? = null
    ): Activity {
        return Activity(
            id = UUID.randomUUID(),
            type = type,
            entityType = "task",
            entityId = UUID.randomUUID(),
            description = "Test activity",
            metadata = emptyMap(),
            createdAt = Instant.now(),
            status = ActivityStatus.SUCCESS,
            projectId = projectId
        )
    }
}
