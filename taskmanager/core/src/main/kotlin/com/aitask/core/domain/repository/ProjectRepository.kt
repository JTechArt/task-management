package com.aitask.core.domain.repository

import com.aitask.core.domain.model.Project
import java.util.UUID

interface ProjectRepository {
    suspend fun findById(id: UUID): Project?
    suspend fun findAll(): List<Project>
    suspend fun findAllActive(): List<Project>
    suspend fun findByName(name: String): Project?
    suspend fun create(project: Project): Project
    suspend fun update(project: Project): Project
    suspend fun archive(id: UUID): Project?
    suspend fun delete(id: UUID)
}

