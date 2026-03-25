package com.aitask.core.data.repository

import com.aitask.core.data.entity.AgentDefinitions
import com.aitask.core.domain.model.AgentDefinition
import com.aitask.core.domain.model.AgentDefinitionRequest
import com.aitask.core.domain.model.AgentScope
import com.aitask.core.domain.model.AgentTrigger
import com.aitask.core.domain.repository.AgentDefinitionRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import java.time.Instant
import java.util.UUID

class AgentDefinitionRepositoryImpl : AgentDefinitionRepository {
    override suspend fun findAll(): List<AgentDefinition> = newSuspendedTransaction {
        AgentDefinitions.selectAll()
            .orderBy(AgentDefinitions.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findById(id: UUID): AgentDefinition? = newSuspendedTransaction {
        AgentDefinitions.selectAll()
            .where { AgentDefinitions.id eq id }
            .map { it.toDomain() }
            .singleOrNull()
    }

    override suspend fun findByProject(projectId: UUID): List<AgentDefinition> = newSuspendedTransaction {
        AgentDefinitions.selectAll()
            .where { AgentDefinitions.projectId eq projectId }
            .orderBy(AgentDefinitions.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findGlobal(): List<AgentDefinition> = newSuspendedTransaction {
        AgentDefinitions.selectAll()
            .where { AgentDefinitions.projectId.isNull() }
            .orderBy(AgentDefinitions.createdAt to SortOrder.DESC)
            .map { it.toDomain() }
    }

    override suspend fun findAvailableForProject(projectId: UUID?): List<AgentDefinition> = newSuspendedTransaction {
        val query = AgentDefinitions.selectAll().where {
            if (projectId == null) {
                (AgentDefinitions.isEnabled eq true) and AgentDefinitions.projectId.isNull()
            } else {
                (AgentDefinitions.isEnabled eq true) and (
                    AgentDefinitions.projectId.isNull() or (AgentDefinitions.projectId eq projectId)
                )
            }
        }.orderBy(AgentDefinitions.createdAt to SortOrder.DESC)
        query.map { it.toDomain() }
    }

    override suspend fun save(request: AgentDefinitionRequest): AgentDefinition = newSuspendedTransaction {
        val existing = request.id?.let { findRowById(it) }
        val now = Instant.now()
        val id = request.id ?: UUID.randomUUID()
        if (existing == null) {
            AgentDefinitions.insert {
                it[AgentDefinitions.id] = id
                it[name] = request.name.trim()
                it[description] = request.description?.takeIf { text -> text.isNotBlank() }
                it[promptTemplate] = request.promptTemplate.trim()
                it[llmConfigurationId] = request.llmConfigurationId
                it[scope] = request.scope.name
                it[projectId] = request.projectId
                it[trigger] = request.trigger.name
                it[isEnabled] = request.isEnabled
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            AgentDefinitions.update({ AgentDefinitions.id eq id }) {
                it[name] = request.name.trim()
                it[description] = request.description?.takeIf { text -> text.isNotBlank() }
                it[promptTemplate] = request.promptTemplate.trim()
                it[llmConfigurationId] = request.llmConfigurationId
                it[scope] = request.scope.name
                it[projectId] = request.projectId
                it[trigger] = request.trigger.name
                it[isEnabled] = request.isEnabled
                it[updatedAt] = now
            }
        }
        findRowById(id)?.toDomain() ?: error("Saved agent definition could not be reloaded")
    }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        AgentDefinitions.deleteWhere { AgentDefinitions.id eq id } > 0
    }

    private fun ResultRow.toDomain(): AgentDefinition = AgentDefinition(
        id = this[AgentDefinitions.id].value,
        name = this[AgentDefinitions.name],
        description = this[AgentDefinitions.description],
        promptTemplate = this[AgentDefinitions.promptTemplate],
        llmConfigurationId = this[AgentDefinitions.llmConfigurationId]?.value,
        scope = AgentScope.valueOf(this[AgentDefinitions.scope]),
        projectId = this[AgentDefinitions.projectId]?.value,
        trigger = AgentTrigger.valueOf(this[AgentDefinitions.trigger]),
        isEnabled = this[AgentDefinitions.isEnabled],
        createdAt = this[AgentDefinitions.createdAt],
        updatedAt = this[AgentDefinitions.updatedAt]
    )

    private fun findRowById(id: UUID): ResultRow? =
        AgentDefinitions.selectAll()
            .where { AgentDefinitions.id eq id }
            .singleOrNull()
}
