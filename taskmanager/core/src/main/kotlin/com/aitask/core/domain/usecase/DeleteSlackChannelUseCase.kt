package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.SlackChannelRepository
import java.util.UUID

class DeleteSlackChannelUseCase(
    private val slackChannelRepository: SlackChannelRepository
) {
    suspend operator fun invoke(id: UUID): Result<Unit> {
        val existing = slackChannelRepository.findById(id)
            ?: return Result.failure(IllegalArgumentException("Slack channel config not found: $id"))
        slackChannelRepository.delete(id)
        return Result.success(Unit)
    }
}
