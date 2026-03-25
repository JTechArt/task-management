package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.repository.AgentDefinitionRepository
import java.util.UUID

class GetAgentDefinitionsUseCase(
    private val agentDefinitionRepository: AgentDefinitionRepository
) {
    suspend operator fun invoke(projectId: UUID? = null): Result<List<AgentDefinition>> = runCatching {
        agentDefinitionRepository.findAvailableForProject(projectId)
    }
}

