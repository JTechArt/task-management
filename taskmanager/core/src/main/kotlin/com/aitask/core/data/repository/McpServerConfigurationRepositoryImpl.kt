package com.aitask.core.data.repository

import com.aitask.core.data.entity.McpServerConfigurations
import com.aitask.core.domain.model.McpServerConfiguration
import com.aitask.core.domain.model.McpServerConfigurationRequest
import com.aitask.core.domain.repository.McpServerConfigurationRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class McpServerConfigurationRepositoryImpl : McpServerConfigurationRepository {
    override suspend fun find(): McpServerConfiguration? = newSuspendedTransaction {
        findRow()?.toDomain()
    }

    override suspend fun save(request: McpServerConfigurationRequest): McpServerConfiguration = newSuspendedTransaction {
        val existing = findRow()
        val id = existing?.get(McpServerConfigurations.id)?.value ?: request.id ?: UUID.randomUUID()
        val now = Instant.now()

        if (existing == null) {
            McpServerConfigurations.insert {
                it[McpServerConfigurations.id] = id
                it[isEnabled] = request.isEnabled
                it[port] = request.port
                it[createdAt] = now
                it[updatedAt] = now
            }
        } else {
            McpServerConfigurations.update({ McpServerConfigurations.id eq id }) {
                it[isEnabled] = request.isEnabled
                it[port] = request.port
                it[updatedAt] = now
            }
        }

        findRow()?.toDomain() ?: error("Saved MCP configuration could not be reloaded")
    }

    private fun ResultRow.toDomain(): McpServerConfiguration = McpServerConfiguration(
        id = this[McpServerConfigurations.id].value,
        isEnabled = this[McpServerConfigurations.isEnabled],
        port = this[McpServerConfigurations.port],
        createdAt = this[McpServerConfigurations.createdAt],
        updatedAt = this[McpServerConfigurations.updatedAt]
    )

    private fun findRow(): ResultRow? =
        McpServerConfigurations.selectAll()
            .lastOrNull()
}
