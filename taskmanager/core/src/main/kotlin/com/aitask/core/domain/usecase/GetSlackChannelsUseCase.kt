package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.SlackChannelConfig
import com.aitask.core.domain.repository.SlackChannelRepository
import java.util.UUID

class GetSlackChannelsUseCase(
    private val slackChannelRepository: SlackChannelRepository
) {
    suspend operator fun invoke(projectId: UUID): List<SlackChannelConfig> =
        slackChannelRepository.findByProject(projectId)
}
