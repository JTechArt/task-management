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

class UpdateRepositoryUseCaseTest {
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var useCase: UpdateRepositoryUseCase
    
    @BeforeEach
    fun setup() {
        repositoryRepository = mockk()
        useCase = UpdateRepositoryUseCase(repositoryRepository)
    }
    
    @Test
    fun `should update repository when valid`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val existing = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "old-name",
            cloneUrl = "https://github.com/old/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = UpdateRepositoryRequest(
            name = "new-name",
            cloneUrl = "https://github.com/new/repo.git"
        )
        
        coEvery { repositoryRepository.findById(repositoryId) } returns existing
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existing)
        coEvery { repositoryRepository.update(any()) } answers { firstArg() }
        
        // When
        val result = useCase(repositoryId, request)
        
        // Then
        assertTrue(result.isSuccess)
        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals("new-name", updated?.name)
        assertEquals("https://github.com/new/repo.git", updated?.cloneUrl)
        coVerify { repositoryRepository.update(any()) }
    }
    
    @Test
    fun `should fail when repository does not exist`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val request = UpdateRepositoryRequest(name = "new-name")
        
        coEvery { repositoryRepository.findById(repositoryId) } returns null
        
        // When
        val result = useCase(repositoryId, request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RepositoryNotFoundException)
    }
    
    @Test
    fun `should fail when name is duplicate`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val existing = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "repo-1",
            cloneUrl = "https://github.com/repo1/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val otherRepo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "repo-2",
            cloneUrl = "https://github.com/repo2/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = UpdateRepositoryRequest(name = "repo-2") // Duplicate name
        
        coEvery { repositoryRepository.findById(repositoryId) } returns existing
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existing, otherRepo)
        
        // When
        val result = useCase(repositoryId, request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DuplicateRepositoryException)
    }
    
    @Test
    fun `should fail when trying to set as primary but primary already exists`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val existing = Repository(
            id = repositoryId,
            projectId = projectId,
            name = "repo-1",
            cloneUrl = "https://github.com/repo1/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = false,
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

        val request = UpdateRepositoryRequest(isPrimary = true)

        coEvery { repositoryRepository.findById(repositoryId) } returns existing
        coEvery { repositoryRepository.findPrimaryByProject(projectId) } returns primaryRepo

        // When
        val result = useCase(repositoryId, request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PrimaryRepositoryException)
    }

    @Test
    fun `should fail when trying to unset primary on only repository`() = runTest {
        // Given
        val repositoryId = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val existing = Repository(
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

        val request = UpdateRepositoryRequest(isPrimary = false)

        coEvery { repositoryRepository.findById(repositoryId) } returns existing
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existing)

        // When
        val result = useCase(repositoryId, request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PrimaryRepositoryException)
    }
}

