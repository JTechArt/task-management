package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ActivityRepository
import com.aitask.core.domain.repository.CodexConfigurationRepository
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.SlackNotificationService
import com.aitask.core.domain.usecase.*
import io.mockk.*
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
    private lateinit var activityRepository: ActivityRepository
    private lateinit var getAgentDefinitionsUseCase: GetAgentDefinitionsUseCase
    private lateinit var runAgentUseCase: RunAgentUseCase

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
        val launchCodexUseCase = mockk<LaunchCodexUseCase>(relaxed = true)
        val codexConfigurationRepository = mockk<CodexConfigurationRepository>(relaxed = true)
        val generateTaskContentUseCase = mockk<GenerateTaskContentUseCase>()
        val generateGitAssistantSuggestionUseCase = mockk<GenerateGitAssistantSuggestionUseCase>()
        getAgentDefinitionsUseCase = mockk()
        runAgentUseCase = mockk()
        val ideService = mockk<IDEService>()
        val repositoryRepository = mockk<com.aitask.core.domain.repository.RepositoryRepository>(relaxed = true)
        projectRepository = mockk()
        activityRepository = mockk(relaxed = true)
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
        coEvery { getAgentDefinitionsUseCase.invoke(any()) } returns Result.success(emptyList())
        coEvery { codexConfigurationRepository.find() } returns null
        coEvery { generateTaskContentUseCase.invoke(any()) } returns Result.success(
            TaskContentGenerationResult(
                generatedText = "Generated description",
                configurationName = "Local Ollama",
                modelIdentifier = "llama3.1",
                endpointUrl = "http://localhost:11434"
            )
        )
        coEvery { generateGitAssistantSuggestionUseCase.invoke(any()) } returns Result.success(
            GitAssistantSuggestionResult(
                generatedText = "feat: add git assistant",
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
            launchCodexUseCase = launchCodexUseCase,
            codexConfigurationRepository = codexConfigurationRepository,
            generateTaskContentUseCase = generateTaskContentUseCase,
            generateGitAssistantSuggestionUseCase = generateGitAssistantSuggestionUseCase,
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = ideService,
            repositoryRepository = repositoryRepository,
            projectRepository = projectRepository,
            slackNotificationService = slackNotificationService,
            activityRepository = activityRepository,
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
    fun `generateGitAssistantSuggestion stores suggestion and state`() = runTest(testDispatcher) {
        viewModel.generateGitAssistantSuggestion(
            taskId = TEST_TASK_ID,
            mode = GitAssistantSuggestionMode.COMMIT_MESSAGE
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeneratingGitAssistantSuggestion)
        assertEquals("feat: add git assistant", viewModel.uiState.gitAssistantSuggestion)
        assertEquals(GitAssistantSuggestionMode.COMMIT_MESSAGE, viewModel.uiState.gitAssistantSuggestionMode)
        assertTrue(viewModel.uiState.gitAssistantSuggestionTargetTaskId == TEST_TASK_ID)
    }

    @Test
    fun `markGitAssistantSuggestionUsed records activity and clears suggestion state`() = runTest(testDispatcher) {
        val task = Task(
            id = TEST_TASK_ID,
            title = "Add git assistant",
            description = "Draft commit message and PR text",
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = TEST_PROJECT_ID,
            workspacePath = "/workspace",
            branchName = "feature/git-assist",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val generateGitAssistantSuggestionUseCase = mockk<GenerateGitAssistantSuggestionUseCase>()
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
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(listOf(task))
        coEvery { generateGitAssistantSuggestionUseCase.invoke(any()) } returns Result.success(
            GitAssistantSuggestionResult(
                generatedText = "feat: add git assistant",
                configurationName = "Local Ollama",
                modelIdentifier = "llama3.1",
                endpointUrl = "http://localhost:11434"
            )
        )

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = generateGitAssistantSuggestionUseCase,
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateGitAssistantSuggestion(taskId = TEST_TASK_ID, mode = GitAssistantSuggestionMode.PR_DESCRIPTION)
        advanceUntilIdle()

        viewModel.markGitAssistantSuggestionUsed(
            taskId = TEST_TASK_ID,
            mode = GitAssistantSuggestionMode.PR_DESCRIPTION,
            suggestion = "PR title and body"
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.gitAssistantSuggestion == null)
        assertTrue(viewModel.uiState.gitAssistantSuggestionError == null)
        assertTrue(viewModel.uiState.gitAssistantSuggestionTargetTaskId == null)
        val activitySlot = slot<Activity>()
        coVerify(exactly = 1) { activityRepository.create(capture(activitySlot)) }
        assertEquals(ActivityType.LLM_SUGGESTION_USED, activitySlot.captured.type)
        assertEquals("task", activitySlot.captured.entityType)
        assertEquals(TEST_TASK_ID, activitySlot.captured.entityId)
        assertEquals(GitAssistantSuggestionMode.PR_DESCRIPTION.name, activitySlot.captured.metadata["mode"])
        assertEquals("Add git assistant", activitySlot.captured.metadata["taskTitle"])
        assertEquals("PR title and body".length.toString(), activitySlot.captured.metadata["suggestionLength"])
        assertEquals("feature/git-assist", activitySlot.captured.metadata["branchName"])
    }

    @Test
    fun `runAgent stores execution result and clears loading state`() = runTest(testDispatcher) {
        val task = Task(
            id = TEST_TASK_ID,
            title = "Add agent builder",
            description = "Create reusable agents",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = TEST_PROJECT_ID,
            workspacePath = "/workspace",
            branchName = "feature/agent-builder",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val agent = AgentDefinition(
            id = UUID.randomUUID(),
            name = "Planner",
            description = null,
            promptTemplate = "Plan {{taskTitle}}",
            llmConfigurationId = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val getAgentDefinitionsUseCase = mockk<GetAgentDefinitionsUseCase>()
        val runAgentUseCase = mockk<RunAgentUseCase>()
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
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(listOf(task))
        coEvery { getAgentDefinitionsUseCase.invoke(any()) } returns Result.success(listOf(agent))
        coEvery { runAgentUseCase.invoke(any()) } returns Result.success(
            RunAgentResult(
                agentId = agent.id,
                agentName = agent.name,
                generatedText = "Plan drafted",
                configurationName = "Local Ollama",
                modelIdentifier = "llama3.1",
                endpointUrl = "http://localhost:11434"
            )
        )

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runAgent(TEST_TASK_ID, agent.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isRunningAgent)
        assertEquals("Plan drafted", viewModel.uiState.agentRunResult)
        assertEquals(agent.id, viewModel.uiState.selectedAgentId)
        assertTrue(viewModel.uiState.agentRunError == null)
    }

    @Test
    fun `runAgent surfaces failure and clears loading state`() = runTest(testDispatcher) {
        val task = Task(
            id = TEST_TASK_ID,
            title = "Add agent builder",
            description = "Create reusable agents",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = TEST_PROJECT_ID,
            workspacePath = "/workspace",
            branchName = "feature/agent-builder",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val agent = AgentDefinition(
            id = UUID.randomUUID(),
            name = "Planner",
            description = null,
            promptTemplate = "Plan {{taskTitle}}",
            llmConfigurationId = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val getAgentDefinitionsUseCase = mockk<GetAgentDefinitionsUseCase>()
        val runAgentUseCase = mockk<RunAgentUseCase>()
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
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(listOf(task))
        coEvery { getAgentDefinitionsUseCase.invoke(any()) } returns Result.success(listOf(agent))
        coEvery { runAgentUseCase.invoke(any()) } returns Result.failure(IllegalStateException("Agent failed"))

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.runAgent(TEST_TASK_ID, agent.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isRunningAgent)
        assertEquals("Agent failed", viewModel.uiState.agentRunError)
        assertTrue(viewModel.uiState.agentRunResult == null)
    }

    @Test
    fun `selectTask auto-runs task opened agents`() = runTest(testDispatcher) {
        val task = Task(
            id = TEST_TASK_ID,
            title = "Add agent builder",
            description = "Create reusable agents",
            taskType = TaskType.FEATURE,
            status = TaskStatus.IN_PROGRESS,
            projectId = TEST_PROJECT_ID,
            workspacePath = "/workspace",
            branchName = "feature/agent-builder",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val agent = AgentDefinition(
            id = UUID.randomUUID(),
            name = "On Open",
            description = null,
            promptTemplate = "Review {{taskTitle}}",
            llmConfigurationId = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.TASK_OPENED,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val getAgentDefinitionsUseCase = mockk<GetAgentDefinitionsUseCase>()
        val runAgentUseCase = mockk<RunAgentUseCase>()
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
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(listOf(task))
        coEvery { getAgentDefinitionsUseCase.invoke(any()) } returns Result.success(listOf(agent))
        coEvery { runAgentUseCase.invoke(any()) } returns Result.success(
            RunAgentResult(
                agentId = agent.id,
                agentName = agent.name,
                generatedText = "Auto-run output",
                configurationName = "Local Ollama",
                modelIdentifier = "llama3.1",
                endpointUrl = "http://localhost:11434"
            )
        )

        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTask(task)
        advanceUntilIdle()

        coVerify(atLeast = 1) { runAgentUseCase.invoke(match { it.taskId == TEST_TASK_ID && it.agentId == agent.id }) }
        assertEquals("Auto-run output", viewModel.uiState.agentRunResult)
        assertFalse(viewModel.uiState.isRunningAgent)
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
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = failingUseCase,
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
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

    @Test
    fun `generateGitAssistantSuggestion surfaces failure and clears loading state`() = runTest(testDispatcher) {
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val failingUseCase = mockk<GenerateGitAssistantSuggestionUseCase>()
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
            launchCodexUseCase = mockk(relaxed = true),
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = failingUseCase,
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateGitAssistantSuggestion(
            taskId = TEST_TASK_ID,
            mode = GitAssistantSuggestionMode.COMMENT_DRAFT
        )

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isGeneratingGitAssistantSuggestion)
        assertEquals("LLM unavailable", viewModel.uiState.gitAssistantSuggestionError)
        assertTrue(viewModel.uiState.gitAssistantSuggestion == null)
    }

    @Test
    fun `runCodex sets success when use case succeeds`() = runTest(testDispatcher) {
        val taskId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val launchCodexUseCase = mockk<LaunchCodexUseCase>()
        coEvery { getProjectsUseCase(includeArchived = false) } returns Result.success(
            listOf(
                Project(
                    id = projectId,
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
        coEvery { launchCodexUseCase(any()) } returns Result.success(
            LaunchCodexResponse(
                task = Task(
                    id = taskId,
                    title = "T",
                    description = null,
                    taskType = TaskType.FEATURE,
                    status = TaskStatus.IN_PROGRESS,
                    projectId = projectId,
                    workspacePath = "/ws",
                    branchName = null,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    completedAt = null
                ),
                activity = Activity(
                    id = UUID.randomUUID(),
                    type = ActivityType.CODEX_LAUNCHED,
                    entityType = "task",
                    entityId = taskId,
                    description = "ok",
                    metadata = emptyMap(),
                    createdAt = Instant.now(),
                    status = ActivityStatus.SUCCESS,
                    projectId = projectId
                )
            )
        )
        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = launchCodexUseCase,
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.runCodex(taskId)
        advanceUntilIdle()
        assertEquals("Launched Codex CLI successfully", viewModel.uiState.codexLaunchSuccess)
        assertFalse(viewModel.uiState.isLaunchingCodex)
        coVerify(atLeast = 1) { launchCodexUseCase(match { it.taskId == taskId }) }
    }

    @Test
    fun `runCodex sets error when use case fails`() = runTest(testDispatcher) {
        val taskId = UUID.randomUUID()
        val getTasksUseCase = mockk<GetTasksUseCase>()
        val getProjectsUseCase = mockk<GetProjectsUseCase>()
        val launchCodexUseCase = mockk<LaunchCodexUseCase>()
        coEvery { getProjectsUseCase(includeArchived = false) } returns Result.success(emptyList())
        coEvery { getTasksUseCase.invoke(any(), any()) } returns Result.success(emptyList())
        coEvery { launchCodexUseCase(any()) } returns Result.failure(IllegalStateException("Codex disabled"))
        viewModel = TasksViewModel(
            getTasksUseCase = getTasksUseCase,
            createTaskUseCase = mockk(relaxed = true),
            updateTaskUseCase = mockk(relaxed = true),
            deleteTaskUseCase = mockk(relaxed = true),
            getProjectsUseCase = getProjectsUseCase,
            generateWorkspaceUseCase = mockk(relaxed = true),
            cleanupWorkspaceUseCase = mockk(relaxed = true),
            launchIDEUseCase = mockk(relaxed = true),
            launchCodexUseCase = launchCodexUseCase,
            codexConfigurationRepository = mockk(relaxed = true),
            generateTaskContentUseCase = mockk(relaxed = true),
            generateGitAssistantSuggestionUseCase = mockk(relaxed = true),
            getAgentDefinitionsUseCase = getAgentDefinitionsUseCase,
            runAgentUseCase = runAgentUseCase,
            ideService = mockk(relaxed = true),
            repositoryRepository = mockk(relaxed = true),
            projectRepository = projectRepository,
            slackNotificationService = mockk(relaxed = true),
            activityRepository = activityRepository,
            scope = CoroutineScope(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.runCodex(taskId)
        advanceUntilIdle()
        assertEquals("Codex disabled", viewModel.uiState.error)
        assertFalse(viewModel.uiState.isLaunchingCodex)
    }

    companion object {
        private val TEST_PROJECT_ID = UUID.randomUUID()
        private val TEST_TASK_ID = UUID.randomUUID()
    }
}
