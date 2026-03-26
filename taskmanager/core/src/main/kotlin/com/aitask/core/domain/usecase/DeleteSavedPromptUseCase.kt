package com.aitask.core.domain.usecase

import com.aitask.core.domain.repository.SavedPromptRepository
import java.util.UUID

class DeleteSavedPromptUseCase(
    private val savedPromptRepository: SavedPromptRepository
) {
    suspend operator fun invoke(id: UUID): Result<Boolean> = runCatching {
        savedPromptRepository.delete(id)
    }
}
