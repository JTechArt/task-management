package com.aitask.core.domain.repository

import com.aitask.core.domain.model.LlmConfiguration
import com.aitask.core.domain.model.LlmConfigurationRequest
import java.util.UUID

interface LlmConfigurationRepository {
    suspend fun findAll(): List<LlmConfiguration>
    suspend fun findById(id: UUID): LlmConfiguration?
    suspend fun findDefault(): LlmConfiguration?
    suspend fun save(request: LlmConfigurationRequest): LlmConfiguration
    suspend fun delete(id: UUID): Boolean
    suspend fun setDefault(id: UUID): LlmConfiguration?
}
