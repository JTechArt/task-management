package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.Project
import com.aitask.core.domain.repository.ProjectRepository

class GetProjectsUseCase(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(includeArchived: Boolean = false): Result<List<Project>> {
        return try {
            val projects = if (includeArchived) {
                projectRepository.findAll()
            } else {
                projectRepository.findAllActive()
            }
            Result.success(projects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

