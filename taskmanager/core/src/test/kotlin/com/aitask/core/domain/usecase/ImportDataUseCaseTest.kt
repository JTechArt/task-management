package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.DataExportBundle
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.ProjectExportItem
import com.aitask.core.domain.model.ProjectRuleExportItem
import com.aitask.core.domain.model.RepositoryExportItem
import com.aitask.core.domain.model.RuleExportItem
import com.aitask.core.domain.model.TaskExportItem
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.repository.RuleRepository
import com.aitask.core.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class ImportDataUseCaseTest {

    private val projectRepository = mockk<ProjectRepository>(relaxed = true)
    private val taskRepository = mockk<TaskRepository>(relaxed = true)
    private val repositoryRepository = mockk<RepositoryRepository>(relaxed = true)
    private val ruleRepository = mockk<RuleRepository>(relaxed = true)
    private val useCase = ImportDataUseCase(
        projectRepository,
        taskRepository,
        repositoryRepository,
        ruleRepository
    )

    @Test
    fun `validate returns conflicts when project name exists`() = runTest {
        coEvery { projectRepository.findAll() } returns listOf(
            Project(
                id = java.util.UUID.randomUUID(),
                name = "Existing",
                description = null,
                workspacePath = "/other",
                branchTemplate = "task-{taskId}",
                retentionPolicy = com.aitask.core.domain.service.RetentionPolicy.KEEP_ALL,
                tags = emptyList(),
                team = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val bundle = DataExportBundle(
            exportedAt = Instant.now().toString(),
            projects = listOf(
                ProjectExportItem(
                    id = "proj-1",
                    name = "Existing",
                    workspacePath = "/new-path"
                )
            ),
            tasks = emptyList(),
            repositories = emptyList(),
            rules = emptyList(),
            projectRules = emptyList()
        )
        val json = Json { prettyPrint = true }.encodeToString(DataExportBundle.serializer(), bundle)
        val result = useCase.validate(json)
        assertTrue(result.isSuccess)
        val validation = result.getOrThrow()
        assertTrue(!validation.isValid)
        assertTrue(validation.conflicts.any { it.message.contains("name already exists") })
    }

    @Test
    fun `apply creates projects tasks repos rules and preserves relationships`() = runTest {
        coEvery { projectRepository.findAll() } returns emptyList()
        coEvery { projectRepository.create(any()) } answers {
            val p = firstArg<Project>()
            p
        }
        coEvery { taskRepository.create(any()) } answers { firstArg() }
        coEvery { repositoryRepository.create(any()) } answers { firstArg() }
        coEvery { ruleRepository.create(any()) } answers { firstArg() }
        coEvery { ruleRepository.attachToProject(any(), any()) } returns mockk()
        val bundle = DataExportBundle(
            exportedAt = Instant.now().toString(),
            projects = listOf(
                ProjectExportItem(
                    id = "p1",
                    name = "Proj1",
                    workspacePath = "/ws1"
                )
            ),
            tasks = listOf(
                TaskExportItem(
                    id = "t1",
                    projectId = "p1",
                    title = "Task",
                    taskType = "FEATURE",
                    status = "PENDING"
                )
            ),
            repositories = listOf(
                RepositoryExportItem(
                    id = "r1",
                    projectId = "p1",
                    name = "repo",
                    cloneUrl = "https://github.com/u/r.git",
                    provider = "GITHUB",
                    authType = "HTTPS"
                )
            ),
            rules = listOf(
                RuleExportItem(
                    id = "rule1",
                    name = "R1",
                    content = "c",
                    scope = "GLOBAL"
                )
            ),
            projectRules = listOf(
                ProjectRuleExportItem(projectId = "p1", ruleId = "rule1")
            )
        )
        val json = Json { prettyPrint = true }.encodeToString(DataExportBundle.serializer(), bundle)
        val result = useCase.apply(json)
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.projectsCreated)
        assertEquals(1, importResult.tasksCreated)
        assertEquals(1, importResult.reposCreated)
        assertEquals(1, importResult.rulesCreated)
        assertEquals(1, importResult.projectRulesLinked)
        coVerify(atLeast = 1) { projectRepository.create(any()) }
        coVerify(atLeast = 1) { ruleRepository.attachToProject(any(), any()) }
    }

    @Test
    fun `apply fails for invalid JSON`() = runTest {
        val result = useCase.apply("not valid json")
        assertTrue(result.isFailure)
    }
}
