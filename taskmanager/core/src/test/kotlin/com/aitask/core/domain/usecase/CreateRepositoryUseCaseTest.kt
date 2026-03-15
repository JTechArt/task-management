package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.validation.RepositoryValidator
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CreateRepositoryUseCaseTest {
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var repositoryValidator: RepositoryValidator
    private lateinit var useCase: CreateRepositoryUseCase
    
    @BeforeEach
    fun setup() {
        repositoryRepository = mockk()
        projectRepository = mockk()
        repositoryValidator = RepositoryValidator()
        useCase = CreateRepositoryUseCase(repositoryRepository, projectRepository, repositoryValidator)
    }
    
    @Test
    fun `should create repository when valid and project exists`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = CreateRepositoryRequest(
            projectId = projectId,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns emptyList()
        coEvery { repositoryRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isSuccess)
        val repository = result.getOrNull()
        assertNotNull(repository)
        assertEquals("test-repo", repository?.name)
        assertEquals(true, repository?.isPrimary) // First repository should be primary
        coVerify { repositoryRepository.create(any()) }
    }
    
    @Test
    fun `should fail when project does not exist`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val request = CreateRepositoryRequest(
            projectId = projectId,
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        coEvery { projectRepository.findById(projectId) } returns null
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProjectNotFoundException)
    }
    
    @Test
    fun `should fail when repository name is duplicate`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val existingRepo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "test-repo",
            cloneUrl = "https://github.com/existing/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val request = CreateRepositoryRequest(
            projectId = projectId,
            name = "test-repo", // Duplicate name
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existingRepo)
        
        // When
        val result = useCase(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DuplicateRepositoryException)
    }
    
    @Test
    fun `should fail when clone URL is duplicate`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val existingRepo = Repository(
            id = UUID.randomUUID(),
            projectId = projectId,
            name = "existing-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val request = CreateRepositoryRequest(
            projectId = projectId,
            name = "different-name",
            cloneUrl = "https://github.com/test/repo.git", // Duplicate URL
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )

        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existingRepo)

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DuplicateRepositoryException)
    }

    @Test
    fun `should fail when trying to set as primary but primary already exists`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            tags = emptyList(),
            team = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val existingPrimary = Repository(
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

        val request = CreateRepositoryRequest(
            projectId = projectId,
            name = "new-repo",
            cloneUrl = "https://github.com/new/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR),
            isPrimary = true
        )

        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { repositoryRepository.findByProject(projectId) } returns listOf(existingPrimary)
        coEvery { repositoryRepository.findPrimaryByProject(projectId) } returns existingPrimary

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PrimaryRepositoryException)
    }
}
