package com.aitask.desktop.ui.viewmodel

import com.aitask.core.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Tests for repository selection logic
 */
class RepositorySelectionTest {
    
    @Test
    fun `should select primary repository by default when available`() {
        // Given
        val projectId = UUID.randomUUID()
        val primaryRepo = createRepository(
            projectId = projectId,
            name = "primary-repo",
            isPrimary = true
        )
        val secondaryRepo = createRepository(
            projectId = projectId,
            name = "secondary-repo",
            isPrimary = false
        )
        val repositories = listOf(primaryRepo, secondaryRepo)
        
        // When
        val defaultSelection = selectDefaultRepositories(repositories)
        
        // Then
        assertEquals(1, defaultSelection.size)
        assertTrue(defaultSelection.contains(primaryRepo.id))
        assertFalse(defaultSelection.contains(secondaryRepo.id))
    }
    
    @Test
    fun `should select all repositories when no primary exists`() {
        // Given
        val projectId = UUID.randomUUID()
        val repo1 = createRepository(
            projectId = projectId,
            name = "repo-1",
            isPrimary = false
        )
        val repo2 = createRepository(
            projectId = projectId,
            name = "repo-2",
            isPrimary = false
        )
        val repositories = listOf(repo1, repo2)
        
        // When
        val defaultSelection = selectDefaultRepositories(repositories)
        
        // Then
        assertEquals(2, defaultSelection.size)
        assertTrue(defaultSelection.contains(repo1.id))
        assertTrue(defaultSelection.contains(repo2.id))
    }
    
    @Test
    fun `should return empty list when no repositories available`() {
        // Given
        val repositories = emptyList<Repository>()
        
        // When
        val defaultSelection = selectDefaultRepositories(repositories)
        
        // Then
        assertTrue(defaultSelection.isEmpty())
    }
    
    @Test
    fun `should select only primary repository when multiple repositories exist`() {
        // Given
        val projectId = UUID.randomUUID()
        val repositories = listOf(
            createRepository(projectId, "repo-1", false),
            createRepository(projectId, "repo-2", true),
            createRepository(projectId, "repo-3", false),
            createRepository(projectId, "repo-4", false)
        )
        
        // When
        val defaultSelection = selectDefaultRepositories(repositories)
        
        // Then
        assertEquals(1, defaultSelection.size)
        val primaryRepo = repositories.find { it.isPrimary }
        assertNotNull(primaryRepo)
        assertTrue(defaultSelection.contains(primaryRepo!!.id))
    }
    
    // Helper function that mimics the logic in TasksViewModel.showRepositorySelectionDialog
    private fun selectDefaultRepositories(repositories: List<Repository>): List<UUID> {
        return if (repositories.isNotEmpty()) {
            val primary = repositories.find { it.isPrimary }
            if (primary != null) {
                listOf(primary.id)
            } else {
                repositories.map { it.id }
            }
        } else {
            emptyList()
        }
    }
    
    private fun createRepository(
        projectId: UUID,
        name: String,
        isPrimary: Boolean
    ): Repository {
        return Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = name,
            cloneUrl = "https://github.com/test/$name.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = isPrimary,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}

