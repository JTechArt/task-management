package com.aitask.core.data.repository

import com.aitask.core.data.entity.SlackChannels
import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.TaskEvent
import com.aitask.core.domain.repository.SlackChannelRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

private fun parseEnabledEvents(value: String): Set<TaskEvent> {
    if (value.isBlank()) return emptySet()
    return value.split(",").mapNotNull { token ->
        runCatching { TaskEvent.valueOf(token.trim()) }.getOrNull()
    }.toSet()
}

private fun serializeEnabledEvents(events: Set<TaskEvent>): String =
    events.joinToString(",") { it.name }

class SlackChannelRepositoryImpl : SlackChannelRepository {
    override suspend fun findByProject(projectId: UUID): List<SlackChannelConfig> =
        newSuspendedTransaction {
            SlackChannels.selectAll()
                .where { SlackChannels.projectId eq projectId }
                .map { it.toSlackChannelConfig() }
        }

    override suspend fun findByProjectAndEvent(
        projectId: UUID,
        event: TaskEvent
    ): List<SlackChannelConfig> = newSuspendedTransaction {
        SlackChannels.selectAll()
            .where { SlackChannels.projectId eq projectId }
            .map { it.toSlackChannelConfig() }
            .filter { it.hasEventEnabled(event) }
    }

    override suspend fun findById(id: UUID): SlackChannelConfig? = newSuspendedTransaction {
        SlackChannels.selectAll().where { SlackChannels.id eq id }
            .map { it.toSlackChannelConfig() }
            .singleOrNull()
    }

    override suspend fun create(config: SlackChannelConfig): SlackChannelConfig =
        newSuspendedTransaction {
            SlackChannels.insert {
                it[id] = config.id
                it[webhookUrl] = config.webhookUrl
                it[channelDisplayName] = config.channelDisplayName
                it[projectId] = config.projectId
                it[enabledEvents] = serializeEnabledEvents(config.enabledEvents)
                it[createdAt] = config.createdAt
                it[updatedAt] = config.updatedAt
            }
            config
        }

    override suspend fun update(config: SlackChannelConfig): SlackChannelConfig =
        newSuspendedTransaction {
            val now = Instant.now()
            SlackChannels.update({ SlackChannels.id eq config.id }) {
                it[webhookUrl] = config.webhookUrl
                it[channelDisplayName] = config.channelDisplayName
                it[enabledEvents] = serializeEnabledEvents(config.enabledEvents)
                it[updatedAt] = now
            }
            config.copy(updatedAt = now)
        }

    override suspend fun delete(id: UUID): Boolean = newSuspendedTransaction {
        val deleted = SlackChannels.deleteWhere { SlackChannels.id eq id }
        deleted > 0
    }

    override suspend fun deleteByProject(projectId: UUID): Int = newSuspendedTransaction {
        SlackChannels.deleteWhere { SlackChannels.projectId eq projectId }
    }

    private fun ResultRow.toSlackChannelConfig(): SlackChannelConfig = SlackChannelConfig(
        id = this[SlackChannels.id].value,
        webhookUrl = this[SlackChannels.webhookUrl],
        channelDisplayName = this[SlackChannels.channelDisplayName],
        projectId = this[SlackChannels.projectId].value,
        enabledEvents = parseEnabledEvents(this[SlackChannels.enabledEvents]),
        createdAt = this[SlackChannels.createdAt],
        updatedAt = this[SlackChannels.updatedAt]
    )
}
