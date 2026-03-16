package com.aitask.desktop.di

import com.aitask.core.data.repository.*
import com.aitask.core.domain.repository.*
import com.aitask.core.domain.service.GitService
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.RuleApplicationService
import com.aitask.core.domain.service.WorkspaceService
import com.aitask.core.domain.usecase.*
import com.aitask.core.domain.validation.ProjectValidator
import com.aitask.core.domain.validation.RepositoryValidator
import com.aitask.core.domain.validation.RuleValidator
import com.aitask.core.domain.validation.TaskValidator
import com.aitask.core.infrastructure.git.JGitService
import com.aitask.core.infrastructure.health.HealthCheckServiceImpl
import com.aitask.core.infrastructure.ide.DesktopIDEService
import com.aitask.core.infrastructure.rules.FileSystemRuleApplicationService
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
        ActivityRepositoryImpl()
    }

    val ruleRepository: RuleRepository by lazy {
        RuleRepositoryImpl()
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

    val ruleValidator: RuleValidator by lazy {
        RuleValidator()
    }

    // Services
    val gitService: GitService by lazy {
        JGitService()
    }

    val ideService: IDEService by lazy {
        DesktopIDEService()
    }

    val healthCheckService: HealthCheckService by lazy {
        HealthCheckServiceImpl(
            projectRepository,
            repositoryRepository,
            gitService,
            ideService
        )
    }

    val workspaceService: WorkspaceService by lazy {
        FileSystemWorkspaceService(gitService)
    }

    val ruleApplicationService: RuleApplicationService by lazy {
        FileSystemRuleApplicationService()
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
            activityRepository,
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
    val applyRulesToWorkspaceUseCase: ApplyRulesToWorkspaceUseCase by lazy {
        ApplyRulesToWorkspaceUseCase(
            ruleRepository,
            projectRepository,
            ruleApplicationService,
            activityRepository
        )
    }

    val generateWorkspaceUseCase: GenerateWorkspaceUseCase by lazy {
        GenerateWorkspaceUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            workspaceService,
            applyRulesToWorkspaceUseCase,
            activityRepository
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

    // Rule Use Cases
    val createRuleUseCase: CreateRuleUseCase by lazy {
        CreateRuleUseCase(ruleRepository, ruleValidator)
    }

    val updateRuleUseCase: UpdateRuleUseCase by lazy {
        UpdateRuleUseCase(ruleRepository, ruleValidator)
    }

    val deleteRuleUseCase: DeleteRuleUseCase by lazy {
        DeleteRuleUseCase(ruleRepository)
    }

    val getRulesUseCase: GetRulesUseCase by lazy {
        GetRulesUseCase(ruleRepository)
    }

    val attachRuleUseCase: AttachRuleUseCase by lazy {
        AttachRuleUseCase(ruleRepository, projectRepository)
    }

    val detachRuleUseCase: DetachRuleUseCase by lazy {
        DetachRuleUseCase(ruleRepository)
    }

    val exportRuleUseCase: ExportRuleUseCase by lazy {
        ExportRuleUseCase(ruleRepository)
    }

    val importRuleUseCase: ImportRuleUseCase by lazy {
        ImportRuleUseCase(ruleRepository, ruleValidator)
    }
}

