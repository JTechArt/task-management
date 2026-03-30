package com.aitask.core.domain.repository

import com.aitask.core.domain.model.ClaudeConfiguration
import com.aitask.core.domain.model.ClaudeConfigurationRequest
import java.util.UUID

interface ClaudeConfigurationRepository {
    suspend fun find(): ClaudeConfiguration?
    suspend fun save(request: ClaudeConfigurationRequest): ClaudeConfiguration
    suspend fun findApiKey(id: UUID): String?
}
