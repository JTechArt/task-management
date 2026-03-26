package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.SavedPrompt
import com.aitask.core.domain.model.SavedPromptRequest
import com.aitask.core.domain.repository.SavedPromptRepository

class SaveSavedPromptUseCase(
    private val savedPromptRepository: SavedPromptRepository
) {
    suspend operator fun invoke(request: SavedPromptRequest): Result<SavedPrompt> = runCatching {
        savedPromptRepository.save(request)
    }
}
