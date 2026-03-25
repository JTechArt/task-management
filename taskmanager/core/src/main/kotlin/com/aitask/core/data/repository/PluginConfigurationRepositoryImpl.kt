package com.aitask.core.data.repository

import com.aitask.core.data.entity.PluginConfigurations
import com.aitask.core.domain.plugin.PluginConfigurationScope
import com.aitask.core.domain.plugin.PluginConfigurationSnapshot
import com.aitask.core.domain.repository.PluginConfigurationRepository
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

private val json = Json { ignoreUnknownKeys = true }

class PluginConfigurationRepositoryImpl : PluginConfigurationRepository {
    override suspend fun find(
        pluginId: String,
        scope: PluginConfigurationScope,
        scopeKey: String?
    ): PluginConfigurationSnapshot? = newSuspendedTransaction {
        val scopeKeyCondition = if (scopeKey == null) {
            PluginConfigurations.scopeKey.isNull()
        } else {
            PluginConfigurations.scopeKey eq scopeKey
        }
        PluginConfigurations.selectAll()
            .where {
                (PluginConfigurations.pluginId eq pluginId) and
                    (PluginConfigurations.scope eq scope.name) and
                    scopeKeyCondition
            }
            .map { it.toSnapshot() }
            .singleOrNull()
    }

    override suspend fun findByPluginId(pluginId: String): List<PluginConfigurationSnapshot> = newSuspendedTransaction {
        PluginConfigurations.selectAll()
            .where { PluginConfigurations.pluginId eq pluginId }
            .map { it.toSnapshot() }
    }

    override suspend fun save(snapshot: PluginConfigurationSnapshot): PluginConfigurationSnapshot = newSuspendedTransaction {
        val payload = json.encodeToString(PluginConfigurationSnapshot.serializer(), snapshot)
        val scopeKeyCondition = if (snapshot.scopeKey == null) {
            PluginConfigurations.scopeKey.isNull()
        } else {
            PluginConfigurations.scopeKey eq snapshot.scopeKey
        }
        val existing = PluginConfigurations.selectAll()
            .where {
                (PluginConfigurations.pluginId eq snapshot.pluginId) and
                    (PluginConfigurations.scope eq snapshot.scope.name) and
                    scopeKeyCondition
            }
            .singleOrNull()

        if (existing == null) {
            PluginConfigurations.insert {
                it[pluginId] = snapshot.pluginId
                it[scope] = snapshot.scope.name
                it[scopeKey] = snapshot.scopeKey
                it[activeConfigurationJson] = payload
                it[lastKnownGoodConfigurationJson] = if (snapshot.validationStatus == com.aitask.core.domain.plugin.PluginConfigurationValidationStatus.VALID) payload else null
                it[validationStatus] = snapshot.validationStatus.name
                it[validationMessage] = snapshot.validationMessage
                it[lastValidatedAt] = snapshot.lastValidatedAtEpochMillis
                it[createdAt] = Instant.ofEpochMilli(snapshot.updatedAtEpochMillis)
                it[updatedAt] = Instant.ofEpochMilli(snapshot.updatedAtEpochMillis)
            }
        } else {
            PluginConfigurations.update({ PluginConfigurations.id eq existing[PluginConfigurations.id] }) {
                it[activeConfigurationJson] = payload
                if (snapshot.validationStatus == com.aitask.core.domain.plugin.PluginConfigurationValidationStatus.VALID) {
                    it[lastKnownGoodConfigurationJson] = payload
                }
                it[validationStatus] = snapshot.validationStatus.name
                it[validationMessage] = snapshot.validationMessage
                it[lastValidatedAt] = snapshot.lastValidatedAtEpochMillis
                it[updatedAt] = Instant.ofEpochMilli(snapshot.updatedAtEpochMillis)
            }
        }
        snapshot
    }

    private fun ResultRow.toSnapshot(): PluginConfigurationSnapshot {
        val active = json.decodeFromString(
            PluginConfigurationSnapshot.serializer(),
            this[PluginConfigurations.activeConfigurationJson]
        )
        return active.copy(
            validationStatus = com.aitask.core.domain.plugin.PluginConfigurationValidationStatus.valueOf(
                this[PluginConfigurations.validationStatus]
            ),
            validationMessage = this[PluginConfigurations.validationMessage],
            lastValidatedAtEpochMillis = this[PluginConfigurations.lastValidatedAt]
        )
    }
}
