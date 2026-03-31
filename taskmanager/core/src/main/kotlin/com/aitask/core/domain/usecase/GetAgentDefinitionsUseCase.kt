package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.TaskType
import com.aitask.core.domain.repository.AgentDefinitionRepository
import java.util.UUID

class GetAgentDefinitionsUseCase(
    private val agentDefinitionRepository: AgentDefinitionRepository
) {
    suspend operator fun invoke(
        projectId: UUID? = null,
        taskType: TaskType? = null
    ): Result<List<AgentDefinition>> = runCatching {
        agentDefinitionRepository.findAvailableForProject(projectId, taskType)
    }
}

/**
 * Story 10.1 alias: lists agent definitions available for a project context.
 */
typealias ListAgentDefinitionsUseCase = GetAgentDefinitionsUseCase

