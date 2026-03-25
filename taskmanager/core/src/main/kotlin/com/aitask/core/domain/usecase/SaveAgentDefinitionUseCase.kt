package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentDefinitionRequest
import com.aitask.core.domain.repository.AgentDefinitionRepository

class SaveAgentDefinitionUseCase(
    private val agentDefinitionRepository: AgentDefinitionRepository
) {
    suspend operator fun invoke(request: AgentDefinitionRequest): Result<AgentDefinition> = runCatching {
        agentDefinitionRepository.save(request)
    }
}

