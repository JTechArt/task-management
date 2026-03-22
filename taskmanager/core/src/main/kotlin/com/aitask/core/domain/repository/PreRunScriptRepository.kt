package com.aitask.core.domain.repository

import com.aitask.core.domain.model.PreRunScript
import java.util.UUID

interface PreRunScriptRepository {
    suspend fun findById(id: UUID): PreRunScript?
    suspend fun findByProject(projectId: UUID): List<PreRunScript>
    suspend fun findByRepository(repositoryId: UUID): List<PreRunScript>
    suspend fun create(script: PreRunScript): PreRunScript
    suspend fun update(script: PreRunScript): PreRunScript
    suspend fun delete(id: UUID)
}
