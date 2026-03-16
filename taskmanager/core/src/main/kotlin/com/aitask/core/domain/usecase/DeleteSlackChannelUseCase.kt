package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.SlackChannelRepository
import java.util.UUID

class DeleteSlackChannelUseCase(
    private val slackChannelRepository: SlackChannelRepository
) {
    suspend operator fun invoke(id: UUID): Result<Unit> {
        val deleted = slackChannelRepository.delete(id)
        return if (deleted) Result.success(Unit) else Result.failure(IllegalArgumentException("Slack channel config not found: $id"))
    }
}
