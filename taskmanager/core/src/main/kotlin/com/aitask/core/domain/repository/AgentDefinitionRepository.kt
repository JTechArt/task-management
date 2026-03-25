package com.aitask.core.domain.repository

import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentDefinitionRequest
import java.util.UUID

interface AgentDefinitionRepository {
    suspend fun findAll(): List<AgentDefinition>
    suspend fun findById(id: UUID): AgentDefinition?
    suspend fun findByProject(projectId: UUID): List<AgentDefinition>
    suspend fun findGlobal(): List<AgentDefinition>
    suspend fun findAvailableForProject(projectId: UUID?): List<AgentDefinition>
    suspend fun save(request: AgentDefinitionRequest): AgentDefinition
    suspend fun delete(id: UUID): Boolean
}
