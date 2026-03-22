package com.aitask.core.domain.repository

import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.model.TaskEvent
import java.util.UUID

interface SlackChannelRepository {
    suspend fun findById(id: UUID): SlackChannelConfig?
    suspend fun findByProject(projectId: UUID): List<SlackChannelConfig>
    suspend fun findByProjectAndEvent(projectId: UUID, event: TaskEvent): List<SlackChannelConfig>
    suspend fun create(config: SlackChannelConfig): SlackChannelConfig
    suspend fun update(config: SlackChannelConfig): SlackChannelConfig
    suspend fun delete(id: UUID)
}
