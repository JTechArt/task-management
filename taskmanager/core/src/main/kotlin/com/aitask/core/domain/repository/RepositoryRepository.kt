package com.aitask.core.domain.repository

import com.aitask.core.domain.model.Repository
import java.util.UUID

interface RepositoryRepository {
    suspend fun findById(id: UUID): Repository?
    suspend fun findByProject(projectId: UUID): List<Repository>
    suspend fun findPrimaryByProject(projectId: UUID): Repository?
    suspend fun create(repository: Repository): Repository
    suspend fun update(repository: Repository): Repository
    suspend fun delete(id: UUID)
}

