package com.aitask.core.domain.repository

import com.aitask.core.domain.model.SavedPrompt
import com.aitask.core.domain.model.SavedPromptRequest
import java.util.UUID

interface SavedPromptRepository {
    suspend fun findAll(): List<SavedPrompt>
    suspend fun findById(id: UUID): SavedPrompt?
    suspend fun findGlobal(): List<SavedPrompt>
    suspend fun findByProject(projectId: UUID): List<SavedPrompt>
    suspend fun findAvailableForProject(projectId: UUID?): List<SavedPrompt>
    suspend fun save(request: SavedPromptRequest): SavedPrompt
    suspend fun delete(id: UUID): Boolean
}
