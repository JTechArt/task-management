package com.aitask.core.domain.usecase

import com.aitask.core.domain.model.*
import com.aitask.core.domain.repository.ProjectRepository
import com.aitask.core.domain.repository.RepositoryRepository
import com.aitask.core.domain.validation.ProjectValidator
import com.aitask.core.domain.validation.RepositoryValidator
import com.aitask.core.domain.validation.ValidationResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CreateProjectUseCaseTest {
    private lateinit var projectRepository: ProjectRepository
    private lateinit var repositoryRepository: RepositoryRepository
    private lateinit var projectValidator: ProjectValidator
    private lateinit var repositoryValidator: RepositoryValidator
    private lateinit var useCase: CreateProjectUseCase
    
    @BeforeEach
    fun setup() {
        projectRepository = mockk()
        repositoryRepository = mockk()
        projectValidator = mockk()
        repositoryValidator = mockk()
        useCase = CreateProjectUseCase(
            projectRepository,
            repositoryRepository,
            projectValidator,
            repositoryValidator
        )
    }
    
    @Test
    fun `should create project and repository when valid`() = runTest {
        // Given
        val projectRequest = CreateProjectRequest(
            name = "Test Project",
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}"
        )
        val repositoryRequest = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        every { projectValidator.validate(projectRequest) } returns ValidationResult.success()
        every { repositoryValidator.validate(repositoryRequest) } returns ValidationResult.success()
        coEvery { projectRepository.findByName(projectRequest.name) } returns null
        coEvery { projectRepository.create(any()) } answers { firstArg() }
        coEvery { repositoryRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = useCase(projectRequest, repositoryRequest)
        
        // Then
        assertTrue(result.isSuccess)
        val (project, repository) = result.getOrThrow()
        assertEquals("Test Project", project.name)
        assertEquals("test-repo", repository.name)
        assertTrue(repository.isPrimary)
        
        coVerify { projectRepository.create(any()) }
        coVerify { repositoryRepository.create(any()) }
    }
    
    @Test
    fun `should fail when project validation fails`() = runTest {
        // Given
        val projectRequest = CreateProjectRequest(
            name = "",
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}"
        )
        val repositoryRequest = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        every { projectValidator.validate(projectRequest) } returns ValidationResult.failure(
            listOf(com.aitask.core.domain.validation.ValidationError("name", "Name is required", "REQUIRED"))
        )
        
        // When
        val result = useCase(projectRequest, repositoryRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        
        coVerify(exactly = 0) { projectRepository.create(any()) }
    }
    
    @Test
    fun `should fail when repository validation fails`() = runTest {
        // Given
        val projectRequest = CreateProjectRequest(
            name = "Test Project",
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}"
        )
        val repositoryRequest = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        every { projectValidator.validate(projectRequest) } returns ValidationResult.success()
        every { repositoryValidator.validate(repositoryRequest) } returns ValidationResult.failure(
            listOf(com.aitask.core.domain.validation.ValidationError("name", "Name is required", "REQUIRED"))
        )
        
        // When
        val result = useCase(projectRequest, repositoryRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
    }
    
    @Test
    fun `should fail when project name already exists`() = runTest {
        // Given
        val projectRequest = CreateProjectRequest(
            name = "Existing Project",
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}"
        )
        val repositoryRequest = CreateRepositoryRequest(
            projectId = UUID.randomUUID(),
            name = "test-repo",
            cloneUrl = "https://github.com/test/repo.git",
            provider = GitProvider.GITHUB,
            authType = AuthType.HTTPS,
            preferredIDEs = listOf(IDEType.CURSOR)
        )
        
        val existingProject = Project(
            id = UUID.randomUUID(),
            name = "Existing Project",
            description = null,
            workspacePath = "/workspace",
            branchTemplate = "task-{taskId}",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        every { projectValidator.validate(projectRequest) } returns ValidationResult.success()
        every { repositoryValidator.validate(repositoryRequest) } returns ValidationResult.success()
        coEvery { projectRepository.findByName(projectRequest.name) } returns existingProject
        
        // When
        val result = useCase(projectRequest, repositoryRequest)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DuplicateProjectException)
    }
}

