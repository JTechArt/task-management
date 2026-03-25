package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.SlackNotificationService
import com.aitask.core.domain.usecase.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
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
class TasksViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: TasksViewModel
    private lateinit var projectRepository: ProjectRepository

    @BeforeEach
    fun setUp() {
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val createTaskUseCase = mockk<CreateTaskUseCase>()
        val updateTaskUseCase = mockk<UpdateTaskUseCase>(relaxed = true)
        val deleteTaskUseCase = mockk<DeleteTaskUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val generateWorkspaceUseCase = mockk<GenerateWorkspaceUseCase>()
        val cleanupWorkspaceUseCase = mockk<CleanupWorkspaceUseCase>()
        val launchIDEUseCase = mockk<LaunchIDEUseCase>()
        val generateTaskContentUseCase = mockk<GenerateTaskContentUseCase>()
        val ideService = mockk<IDEService>()
        val repositoryRepository = mockk<com.aitask.core.domain.repository.RepositoryRepository>(relaxed = true)
        projectRepository = mockk()
        val slackNotificationService = mockk<SlackNotificationService>(relaxed = true)

        coEvery { getProjectsUseCase(includeArchived = false) } returns Result.success(
            listOf(
                Project(
                    id = TEST_PROJECT_ID,
                    name = "Atlas",
                    description = null,
                    workspacePath = "/workspace",
                    branchTemplate = "task-{taskId}",
                    methodology = Methodology.NONE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        )
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(emptyList())
        coEvery { generateTaskContentUseCase.invoke(any()) } returns Result.success(
            TaskContentGenerationResult(
                generatedText = "Generated description",
                configurationName = "Local Ollama",
                modelIdentifier = "llama3.1",
                endpointUrl = "http://localhost:11434"
            )
        )

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = createTaskUseCase,
            updateTaskUseCase = updateTaskUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = generateWorkspaceUseCase,
            cleanupWorkspaceUseCase = cleanupWorkspaceUseCase,
            launchIDEUseCase = launchIDEUseCase,
            generateTaskContentUseCase = generateTaskContentUseCase,
            ideService = ideService,
            repositoryRepository = repositoryRepository,
            projectRepository = projectRepository,
            slackNotificationService = slackNotificationService,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `generateTaskContentSuggestion stores suggestion and state`() = runTest(testDispatcher) {
        viewModel.generateTaskContentSuggestion(
            projectId = TEST_PROJECT_ID,
            title = "Add local LLM generation",
            currentDescription = "Current draft",
            taskType = TaskType.FEATURE,
            mode = TaskContentGenerationMode.DESCRIPTION,
            targetTaskId = null
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeneratingTaskContent)
        assertEquals("Generated description", viewModel.uiState.taskContentSuggestion)
        assertEquals(TaskContentGenerationMode.DESCRIPTION, viewModel.uiState.taskContentGenerationMode)
        assertTrue(viewModel.uiState.taskContentGenerationTargetTaskId == null)
    }

    @Test
    fun `clearTaskContentSuggestion resets generation state`() = runTest(testDispatcher) {
        viewModel.generateTaskContentSuggestion(
            projectId = TEST_PROJECT_ID,
            title = "Add local LLM generation",
            currentDescription = null,
            taskType = TaskType.FEATURE,
            mode = TaskContentGenerationMode.SUMMARY,
            targetTaskId = TEST_TASK_ID
        )
        advanceUntilIdle()

        viewModel.clearTaskContentSuggestion()

        assertTrue(viewModel.uiState.taskContentSuggestion == null)
        assertTrue(viewModel.uiState.taskContentGenerationError == null)
        assertFalse(viewModel.uiState.isGeneratingTaskContent)
    }

    @Test
    fun `generateTaskContentSuggestion surfaces failure and clears loading state`() = runTest(testDispatcher) {
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val failingUseCase = mockk<GenerateTaskContentUseCase>()
        coEvery { getProjectsUseCase(includeArchived = false) } returns Result.success(
            listOf(
                Project(
                    id = TEST_PROJECT_ID,
                    name = "Atlas",
                    description = null,
                    workspacePath = "/workspace",
                    branchTemplate = "task-{taskId}",
                    methodology = Methodology.NONE,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        )
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(emptyList())
        coEvery { failingUseCase.invoke(any()) } returns Result.failure(IllegalStateException("LLM unavailable"))

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            generateTaskContentUseCase = failingUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateTaskContentSuggestion(
            projectId = TEST_PROJECT_ID,
            title = "Add local LLM generation",
            currentDescription = null,
            taskType = TaskType.FEATURE,
            mode = TaskContentGenerationMode.DESCRIPTION,
            targetTaskId = TEST_TASK_ID
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeneratingTaskContent)
        assertEquals("LLM unavailable", viewModel.uiState.taskContentGenerationError)
        assertTrue(viewModel.uiState.taskContentSuggestion == null)
    }

    companion object {
        private val TEST_PROJECT_ID = UUID.randomUUID()
        private val TEST_TASK_ID = UUID.randomUUID()
    }
}
