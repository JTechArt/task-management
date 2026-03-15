package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.RepositoryRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeleteRepositoryUseCaseTest {
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var useCase: DeleteRepositoryUseCase
    
    @BeforeEach
    fun setup() {
        repositoryRepository = mockk()
        useCase = DeleteRepositoryUseCase(repositoryRepository)
    }
    
    @Test
    fun `should delete repository when valid`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val repository = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "repo-to-delete",
            cloneUrl = "https://github.com/delete/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = false, // Not primary
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val primaryRepo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "primary-repo",
            cloneUrl = "https://github.com/primary/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { repositoryRepository.findById(repositoryId) } returns repository
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(primaryRepo, repository)
        coEvery { repositoryRepository.delete(repositoryId) } just Runs
        
        // When
        val result = useCase(repositoryId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { repositoryRepository.delete(repositoryId) }
    }
    
    @Test
    fun `should fail when repository does not exist`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        
        coEvery { repositoryRepository.findById(repositoryId) } returns null
        
        // When
        val result = useCase(repositoryId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RepositoryNotFoundException)
    }
    
    @Test
    fun `should fail when trying to delete the only repository`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val repository = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "only-repo",
            cloneUrl = "https://github.com/only/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { repositoryRepository.findById(repositoryId) } returns repository
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(repository)
        
        // When
        val result = useCase(repositoryId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotDeleteRepositoryException)
        assertTrue(result.exceptionOrNull()?.message?.contains("only repository") == true)
    }
    
    @Test
    fun `should fail when trying to delete primary repository`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val primaryRepo = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "primary-repo",
            cloneUrl = "https://github.com/primary/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val secondaryRepo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "secondary-repo",
            cloneUrl = "https://github.com/secondary/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        coEvery { repositoryRepository.findById(repositoryId) } returns primaryRepo
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(primaryRepo, secondaryRepo)
        
        // When
        val result = useCase(repositoryId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotDeleteRepositoryException)
        assertTrue(result.exceptionOrNull()?.message?.contains("primary repository") == true)
    }
}

