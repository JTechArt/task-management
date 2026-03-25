package com.aitask.core.domain.repository

import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.model.McpServerConfigurationRequest

interface McpServerConfigurationRepository {
    suspend fun find(): McpServerConfiguration?
    suspend fun save(request: McpServerConfigurationRequest): McpServerConfiguration
}
