package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentDefinitionRequest
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.AgentToolKind
import com.aitask.core.domain.model.AgentTrigger
import com.aitask.core.domain.repository.AgentDefinitionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AgentDefinitionUseCasesTest {
    @Test
    fun `SaveAgentDefinitionUseCase delegates to repository`() = runTest {
        val repository = mockk<AgentDefinitionRepository>()
        val useCase = SaveAgentDefinitionUseCase(repository)
        val request = AgentDefinitionRequest(
            name = "Test",
            promptTemplate = "Hello",
            associatedTools = setOf(AgentToolKind.LOCAL_LLM),
            scope = AgentScope.GLOBAL
        )
        val saved = AgentDefinition(
            id = UUID.randomUUID(),
            name = request.name,
            description = null,
            promptTemplate = request.promptTemplate,
            llmConfigurationId = null,
            associatedTools = request.associatedTools,
            taskTypeFilter = null,
            scope = AgentScope.GLOBAL,
            projectId = null,
            trigger = AgentTrigger.MANUAL,
            isEnabled = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { repository.save(request) } returns saved
        val result = useCase(request)
        assertTrue(result.isSuccess)
        assertEquals(saved, result.getOrNull())
        coVerify(exactly = 1) { repository.save(request) }
    }

    @Test
    fun `DeleteAgentDefinitionUseCase delegates to repository`() = runTest {
        val repository = mockk<AgentDefinitionRepository>()
        val useCase = DeleteAgentDefinitionUseCase(repository)
        val id = UUID.randomUUID()
        coEvery { repository.delete(id) } returns true
        val result = useCase(id)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
        coVerify(exactly = 1) { repository.delete(id) }
    }

    @Test
    fun `GetAgentDefinitionsUseCase loads by project`() = runTest {
        val repository = mockk<AgentDefinitionRepository>()
        val useCase = GetAgentDefinitionsUseCase(repository)
        val projectId = UUID.randomUUID()
        coEvery { repository.findAvailableForProject(projectId, null) } returns emptyList()
        val result = useCase(projectId)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull().isNullOrEmpty())
        coVerify(exactly = 1) { repository.findAvailableForProject(projectId) }
    }
}
