package com.aitask.core.domain.service

import com.aitask.core.domain.model.Methodology
import com.aitask.core.domain.model.Project
import com.aitask.core.domain.model.Task
import com.aitask.core.domain.model.TaskStatus
import com.aitask.core.domain.model.TaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class BmadConfigurationResolverTest {
    private val resolver = BmadConfigurationResolver()

    @Test
    fun `should inherit project defaults when task has no overrides`() {
        val project = createProject(
            methodology = Methodology.BMAD,
            toolIds = listOf("agent:dev", "checklist:story-dod-checklist")
        )
        val task = createTask(projectId = project.id)

        val result = resolver.resolve(project, task)

        assertEquals(Methodology.BMAD, result.methodology)
        assertEquals(project.bmadToolIds, result.toolIds)
        assertTrue(result.injectionEnabled)
        assertFalse(result.methodologyOverridden)
        assertFalse(result.toolsOverridden)
        assertFalse(result.injectionOverridden)
    }

    @Test
    fun `should apply task overrides when present`() {
        val project = createProject(
            methodology = Methodology.BMAD,
            toolIds = listOf("agent:dev")
        )
        val task = createTask(projectId = project.id).copy(
            methodologyOverride = Methodology.CUSTOM,
            bmadToolOverrideIds = listOf("task:execute-checklist"),
            bmadInjectionEnabledOverride = false
        )

        val result = resolver.resolve(project, task)

        assertEquals(Methodology.CUSTOM, result.methodology)
        assertEquals(listOf("task:execute-checklist"), result.toolIds)
        assertFalse(result.injectionEnabled)
        assertTrue(result.methodologyOverridden)
        assertTrue(result.toolsOverridden)
        assertTrue(result.injectionOverridden)
    }

    private fun createProject(
        methodology: Methodology,
        toolIds: List<String>
    ) = Project(
        id = UUID.randomUUID(),
        name = "Project",
        description = null,
        workspacePath = "/workspace",
        branchTemplate = "task-{taskId}",
        methodology = methodology,
        bmadToolIds = toolIds,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createTask(projectId: UUID) = Task(
        id = UUID.randomUUID(),
        title = "Task",
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
}
