package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.AgentDefinitionRepository
import java.util.UUID

class DeleteAgentDefinitionUseCase(
    private val agentDefinitionRepository: AgentDefinitionRepository
) {
    suspend operator fun invoke(id: UUID): Result<Boolean> = runCatching {
        agentDefinitionRepository.delete(id)
    }
}

