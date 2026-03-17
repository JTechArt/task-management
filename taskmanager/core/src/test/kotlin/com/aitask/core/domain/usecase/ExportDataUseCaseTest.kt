package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AuthType
import com.aitask.core.domain.model.GitProvider
import com.aitask.core.domain.model.IDEType
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Repository
import com.aitask.core.domain.model.Rule
import com.aitask.core.domain.model.RuleCategory
import com.aitask.core.domain.model.RuleScope
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.repository.TaskRepository
import com.aitask.core.domain.service.RetentionPolicy
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ExportDataUseCaseTest {

    private val projectRepository = mockk<ProjectRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val repositoryRepository = mockk<RepositoryRepository>()
    private val ruleRepository = mockk<RuleRepository>()
    private val useCase = ExportDataUseCase(
        projectRepository,
        taskRepository,
        repositoryRepository,
        ruleRepository
    )

    @Test
    fun `export returns valid JSON with projects tasks repos rules`() = runTest {
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = "Desc",
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            retentionPolicy = RetentionPolicy.KEEP_ALL,
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val task = Task(
            id = UUID.randomUUID(),
            title = "Task 1",
            description = null,
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = projectId,
            workspacePath = null,
            branchName = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            completedAt = null
        )
        val repo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "repo",
            cloneUrl = "https://github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val rule = Rule(
            id = UUID.randomUUID(),
            name = "Rule 1",
            content = "content",
            category = RuleCategory.CODING_STANDARDS,
            scope = RuleScope.GLOBAL,
            targetIDE = IDEType.CURSOR,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { projectRepository.findAllActive() } returns listOf(project)
        coEvery { taskRepository.findByProject(projectId) } returns listOf(task)
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repo)
        coEvery { ruleRepository.findAll() } returns listOf(rule)
        coEvery { ruleRepository.findProjectRules(projectId) } returns emptyList()
        val result = useCase(includeArchived = false)
        assertTrue(result.isSuccess)
        val json = result.getOrThrow()
        val bundle = Json { ignoreUnknownKeys = true }.decodeFromString(
            com.aitask.core.domain.model.DataExportBundle.serializer(),
            json
        )
        assertEquals(1, bundle.projects.size)
        assertEquals("Test Project", bundle.projects[0].name)
        assertEquals(1, bundle.tasks.size)
        assertEquals("Task 1", bundle.tasks[0].title)
        assertEquals(1, bundle.repositories.size)
        assertEquals("repo", bundle.repositories[0].name)
        assertTrue(bundle.repositories[0].cloneUrl.contains("github.com"))
        assertEquals(1, bundle.rules.size)
        assertEquals("Rule 1", bundle.rules[0].name)
    }

    @Test
    fun `export redacts credentials from clone URL`() = runTest {
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "P",
            description = null,
            workspacePath = "/ws",
            branchTemplate = "task-{taskId}",
            retentionPolicy = RetentionPolicy.KEEP_ALL,
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val repo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "r",
            cloneUrl = "https://user:secret@github.com/user/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { projectRepository.findAllActive() } returns listOf(project)
        coEvery { taskRepository.findByProject(projectId) } returns emptyList()
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repo)
        coEvery { ruleRepository.findAll() } returns emptyList()
        coEvery { ruleRepository.findProjectRules(projectId) } returns emptyList()
        val result = useCase()
        assertTrue(result.isSuccess)
        val json = result.getOrThrow()
        assertTrue(!json.contains("secret"))
        assertTrue(json.contains("***"))
    }
}
