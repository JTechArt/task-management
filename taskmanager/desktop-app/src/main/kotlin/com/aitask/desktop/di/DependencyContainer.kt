package com.aitask.desktop.di

import com.aitask.core.data.repository.*
import com.aitask.core.domain.repository.*
import com.aitask.core.domain.service.GitService
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.WorkspaceService
import com.aitask.core.domain.usecase.*
import com.aitask.core.domain.validation.ProjectValidator
import com.aitask.core.domain.validation.RepositoryValidator
import com.aitask.core.domain.validation.TaskValidator
import com.aitask.core.infrastructure.git.JGitService
import com.aitask.core.infrastructure.ide.DesktopIDEService
import com.aitask.core.infrastructure.workspace.FileSystemWorkspaceService

/**
 * Dependency injection container for the desktop application
 * Provides singleton instances of repositories, services, and use cases
 */
object DependencyContainer {
    
    // Repositories
    val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl()
    }
    
    val repositoryRepository: RepositoryRepository by lazy {
        RepositoryRepositoryImpl()
    }
    
    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl()
    }
    
    val activityRepository: ActivityRepository by lazy {
        InMemoryActivityRepository()
    }
    
    // Validators
    val projectValidator: ProjectValidator by lazy {
        ProjectValidator()
    }
    
    val repositoryValidator: RepositoryValidator by lazy {
        RepositoryValidator()
    }
    
    val taskValidator: TaskValidator by lazy {
        TaskValidator()
    }
    
    // Services
    val gitService: GitService by lazy {
        JGitService()
    }

    val ideService: IDEService by lazy {
        DesktopIDEService()
    }

    val workspaceService: WorkspaceService by lazy {
        FileSystemWorkspaceService(gitService)
    }
    
    // Project Use Cases
    val createProjectUseCase: CreateProjectUseCase by lazy {
        CreateProjectUseCase(
            projectRepository,
            repositoryRepository,
            projectValidator,
            repositoryValidator
        )
    }
    
    val getProjectsUseCase: GetProjectsUseCase by lazy {
        GetProjectsUseCase(projectRepository)
    }
    
    val updateProjectUseCase: UpdateProjectUseCase by lazy {
        UpdateProjectUseCase(projectRepository)
    }
    
    val archiveProjectUseCase: ArchiveProjectUseCase by lazy {
        ArchiveProjectUseCase(projectRepository)
    }

    // Repository Use Cases
    val createRepositoryUseCase: CreateRepositoryUseCase by lazy {
        CreateRepositoryUseCase(
            repositoryRepository,
            projectRepository,
            repositoryValidator
        )
    }

    val updateRepositoryUseCase: UpdateRepositoryUseCase by lazy {
        UpdateRepositoryUseCase(repositoryRepository)
    }

    val deleteRepositoryUseCase: DeleteRepositoryUseCase by lazy {
        DeleteRepositoryUseCase(repositoryRepository)
    }

    // Task Use Cases
    val createTaskUseCase: CreateTaskUseCase by lazy {
        CreateTaskUseCase(
            taskRepository,
            projectRepository,
            taskValidator
        )
    }
    
    val getTasksUseCase: GetTasksUseCase by lazy {
        GetTasksUseCase(taskRepository)
    }
    
    val updateTaskUseCase: UpdateTaskUseCase by lazy {
        UpdateTaskUseCase(taskRepository)
    }
    
    val deleteTaskUseCase: DeleteTaskUseCase by lazy {
        DeleteTaskUseCase(taskRepository)
    }
    
    // Workspace Use Cases
    val generateWorkspaceUseCase: GenerateWorkspaceUseCase by lazy {
        GenerateWorkspaceUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            workspaceService
        )
    }
    
    // IDE Use Cases
    val launchIDEUseCase: LaunchIDEUseCase by lazy {
        LaunchIDEUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            ideService,
            activityRepository
        )
    }
}

