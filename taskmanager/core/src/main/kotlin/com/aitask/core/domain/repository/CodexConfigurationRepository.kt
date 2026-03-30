package com.aitask.core.domain.repository

import com.aitask.core.domain.model.CodexConfiguration
import com.aitask.core.domain.model.CodexConfigurationRequest
import java.util.UUID

interface CodexConfigurationRepository {
    suspend fun find(): CodexConfiguration?
    suspend fun save(request: CodexConfigurationRequest): CodexConfiguration
    suspend fun findApiKey(id: UUID): String?
}
