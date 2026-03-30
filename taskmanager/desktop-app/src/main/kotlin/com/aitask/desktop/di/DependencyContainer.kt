package com.aitask.desktop.di

import com.aitask.core.data.repository.*
import com.aitask.core.domain.repository.*
import com.aitask.core.domain.usecase.DeleteSavedPromptUseCase
import com.aitask.core.domain.usecase.SaveSavedPromptUseCase
import com.aitask.core.domain.service.GitService
import com.aitask.core.domain.service.HealthCheckService
import com.aitask.core.domain.service.IDEService
import com.aitask.core.domain.service.CodexCliService
import com.aitask.core.domain.service.McpBridgeService
import com.aitask.core.domain.service.AgentExecutionService
import com.aitask.core.domain.service.GeppaConnectionValidator
import com.aitask.core.domain.service.GeppaPromptOptimizationService
import com.aitask.core.domain.service.GitAssistantSuggestionService
import com.aitask.core.domain.service.LlmConnectionValidator
import com.aitask.core.domain.service.TaskContentGenerationService
import com.aitask.core.domain.service.RuleApplicationService
import com.aitask.core.domain.service.EncryptionService
import com.aitask.core.domain.service.OAuthService
import com.aitask.core.domain.service.PluginManagementService
import com.aitask.core.domain.service.SlackNotificationService
import com.aitask.core.domain.service.SlackService
import com.aitask.core.domain.service.BmadWorkspaceInjectionService
import com.aitask.core.domain.service.BmadConfigurationResolver
import com.aitask.core.domain.service.WorkspaceService
import com.aitask.core.domain.usecase.*
import com.aitask.core.domain.validation.ProjectValidator
import com.aitask.core.domain.validation.RepositoryValidator
import com.aitask.core.domain.validation.RuleValidator
import com.aitask.core.domain.validation.TaskValidator
import com.aitask.core.infrastructure.git.JGitService
import com.aitask.core.infrastructure.health.HealthCheckServiceImpl
import com.aitask.core.infrastructure.ide.DesktopIDEService
import com.aitask.core.infrastructure.codex.DesktopCodexCliService
import com.aitask.core.infrastructure.llm.DefaultAgentExecutionService
import com.aitask.core.infrastructure.llm.DefaultGeppaConnectionValidator
import com.aitask.core.infrastructure.llm.DefaultGeppaPromptOptimizationService
import com.aitask.core.infrastructure.llm.DefaultGitAssistantSuggestionService
import com.aitask.core.infrastructure.llm.DefaultLlmConnectionValidator
import com.aitask.core.infrastructure.llm.DefaultTaskContentGenerationService
import com.aitask.core.infrastructure.mcp.DefaultMcpBridgeService
import com.aitask.core.infrastructure.plugin.DefaultPluginPrerequisiteProbe
import com.aitask.core.infrastructure.plugin.InMemoryPluginManagementService
import com.aitask.core.infrastructure.rules.FileSystemRuleApplicationService
import com.aitask.core.config.OAuthConfig
import com.aitask.core.infrastructure.oauth.OAuthServiceImpl
import com.aitask.core.infrastructure.prerun.LocalPreRunScriptService
import com.aitask.core.infrastructure.security.AesGcmEncryptionService
import com.aitask.core.infrastructure.slack.SlackWebhookClient
import com.aitask.core.infrastructure.workspace.FileSystemBmadWorkspaceInjectionService
import com.aitask.core.infrastructure.workspace.FileSystemWorkspaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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

    val agentDefinitionRepository: AgentDefinitionRepository by lazy {
        AgentDefinitionRepositoryImpl()
    }

    val mcpServerConfigurationRepository: McpServerConfigurationRepository by lazy {
        McpServerConfigurationRepositoryImpl()
    }

    val preRunScriptRepository: PreRunScriptRepository by lazy {
        PreRunScriptRepositoryImpl()
    }

    val ruleRepository: RuleRepository by lazy {
        RuleRepositoryImpl()
    }

    val slackChannelRepository: SlackChannelRepository by lazy {
        SlackChannelRepositoryImpl()
    }

    val encryptionService: EncryptionService by lazy {
        AesGcmEncryptionService()
    }

    val llmConfigurationRepository: LlmConfigurationRepository by lazy {
        LlmConfigurationRepositoryImpl(encryptionService)
    }

    val oauthConnectionRepository: OAuthConnectionRepository by lazy {
        OAuthConnectionRepositoryImpl(encryptionService)
    }

    val oauthService: OAuthService by lazy {
        OAuthServiceImpl(
            OAuthConfig(),
            oauthConnectionRepository,
            encryptionService
        )
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

    val bmadWorkspaceInjectionService: BmadWorkspaceInjectionService by lazy {
        FileSystemBmadWorkspaceInjectionService()
    }

    val bmadConfigurationResolver: BmadConfigurationResolver by lazy {
        BmadConfigurationResolver()
    }

    val ruleApplicationService: RuleApplicationService by lazy {
        FileSystemRuleApplicationService()
    }

    val slackService: SlackService by lazy {
        SlackWebhookClient()
    }

    val slackNotificationService: SlackNotificationService by lazy {
        SlackNotificationService(
            slackService,
            slackChannelRepository,
            activityRepository,
            CoroutineScope(Dispatchers.Default)
        )
    }

    val preRunScriptService by lazy {
        LocalPreRunScriptService()
    }

    val llmConnectionValidator: LlmConnectionValidator by lazy {
        DefaultLlmConnectionValidator()
    }

    val geppaConfigurationRepository: GeppaConfigurationRepository by lazy {
        GeppaConfigurationRepositoryImpl()
    }

    val codexConfigurationRepository: CodexConfigurationRepository by lazy {
        CodexConfigurationRepositoryImpl(encryptionService)
    }

    val codexCliService: CodexCliService by lazy {
        DesktopCodexCliService()
    }

    val savedPromptRepository: SavedPromptRepository by lazy {
        SavedPromptRepositoryImpl()
    }

    val geppaConnectionValidator: GeppaConnectionValidator by lazy {
        DefaultGeppaConnectionValidator()
    }

    val geppaPromptOptimizationService: GeppaPromptOptimizationService by lazy {
        DefaultGeppaPromptOptimizationService(geppaConfigurationRepository)
    }

    val taskContentGenerationService: TaskContentGenerationService by lazy {
        DefaultTaskContentGenerationService(geppaPromptOptimizationService)
    }

    val gitAssistantSuggestionService: GitAssistantSuggestionService by lazy {
        DefaultGitAssistantSuggestionService(geppaPromptOptimizationService)
    }

    val agentExecutionService: AgentExecutionService by lazy {
        DefaultAgentExecutionService()
    }

    val mcpBridgeService: McpBridgeService by lazy {
        DefaultMcpBridgeService(
            settingsRepository = mcpServerConfigurationRepository,
            projectRepository = projectRepository,
            taskRepository = taskRepository,
            repositoryRepository = repositoryRepository
        )
    }

    val pluginManagementService: PluginManagementService by lazy {
        InMemoryPluginManagementService(
            activityRepository = activityRepository,
            configurationRepository = PluginConfigurationRepositoryImpl(),
            prerequisiteProbe = DefaultPluginPrerequisiteProbe(ideService = ideService)
        )
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

    val getAgentDefinitionsUseCase: GetAgentDefinitionsUseCase by lazy {
        GetAgentDefinitionsUseCase(agentDefinitionRepository)
    }
    
    val updateProjectUseCase: UpdateProjectUseCase by lazy {
        UpdateProjectUseCase(projectRepository)
    }
    
    val archiveProjectUseCase: ArchiveProjectUseCase by lazy {
        ArchiveProjectUseCase(projectRepository)
    }
    val unarchiveProjectUseCase: UnarchiveProjectUseCase by lazy {
        UnarchiveProjectUseCase(projectRepository)
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
        UpdateTaskUseCase(taskRepository, activityRepository)
    }
    
    val deleteTaskUseCase: DeleteTaskUseCase by lazy {
        DeleteTaskUseCase(taskRepository, activityRepository)
    }

    val saveAgentDefinitionUseCase: SaveAgentDefinitionUseCase by lazy {
        SaveAgentDefinitionUseCase(agentDefinitionRepository)
    }

    val deleteAgentDefinitionUseCase: DeleteAgentDefinitionUseCase by lazy {
        DeleteAgentDefinitionUseCase(agentDefinitionRepository)
    }

    val saveSavedPromptUseCase: SaveSavedPromptUseCase by lazy {
        SaveSavedPromptUseCase(savedPromptRepository)
    }

    val deleteSavedPromptUseCase: DeleteSavedPromptUseCase by lazy {
        DeleteSavedPromptUseCase(savedPromptRepository)
    }

    val runAgentUseCase: RunAgentUseCase by lazy {
        RunAgentUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            agentDefinitionRepository,
            llmConfigurationRepository,
            agentExecutionService,
            activityRepository,
            geppaPromptOptimizationService
        )
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
            bmadConfigurationResolver,
            bmadWorkspaceInjectionService,
            applyRulesToWorkspaceUseCase,
            activityRepository
        )
    }
    
    val cleanupWorkspaceUseCase: CleanupWorkspaceUseCase by lazy {
        CleanupWorkspaceUseCase(
            taskRepository,
            projectRepository,
            workspaceService,
            activityRepository
        )
    }

    // IDE Use Cases
    val launchIDEUseCase: LaunchIDEUseCase by lazy {
        LaunchIDEUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            preRunScriptRepository,
            preRunScriptService,
            bmadConfigurationResolver,
            ideService,
            activityRepository
        )
    }

    val launchCodexUseCase: LaunchCodexUseCase by lazy {
        LaunchCodexUseCase(
            taskRepository,
            projectRepository,
            preRunScriptRepository,
            preRunScriptService,
            bmadConfigurationResolver,
            codexConfigurationRepository,
            codexCliService,
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

    // Slack Use Cases
    val getSlackChannelsUseCase: GetSlackChannelsUseCase by lazy {
        GetSlackChannelsUseCase(slackChannelRepository)
    }

    val createSlackChannelUseCase: CreateSlackChannelUseCase by lazy {
        CreateSlackChannelUseCase(slackChannelRepository, projectRepository)
    }

    val updateSlackChannelUseCase: UpdateSlackChannelUseCase by lazy {
        UpdateSlackChannelUseCase(slackChannelRepository)
    }

    val deleteSlackChannelUseCase: DeleteSlackChannelUseCase by lazy {
        DeleteSlackChannelUseCase(slackChannelRepository)
    }

    val sendSlackTestMessageUseCase: SendSlackTestMessageUseCase by lazy {
        SendSlackTestMessageUseCase(slackChannelRepository, slackService)
    }

    val getOAuthStatusUseCase: GetOAuthStatusUseCase by lazy {
        GetOAuthStatusUseCase(oauthConnectionRepository, oauthService)
    }

    val generateTaskContentUseCase: GenerateTaskContentUseCase by lazy {
        GenerateTaskContentUseCase(
            projectRepository,
            llmConfigurationRepository,
            taskContentGenerationService
        )
    }

    val generateGitAssistantSuggestionUseCase: GenerateGitAssistantSuggestionUseCase by lazy {
        GenerateGitAssistantSuggestionUseCase(
            taskRepository,
            projectRepository,
            repositoryRepository,
            llmConfigurationRepository,
            gitService,
            gitAssistantSuggestionService
        )
    }

    val exportDataUseCase: ExportDataUseCase by lazy {
        ExportDataUseCase(
            projectRepository,
            taskRepository,
            repositoryRepository,
            ruleRepository
        )
    }

    val importDataUseCase: ImportDataUseCase by lazy {
        ImportDataUseCase(
            projectRepository,
            taskRepository,
            repositoryRepository,
            ruleRepository
        )
    }

    val createBackupUseCase: CreateBackupUseCase by lazy {
        CreateBackupUseCase(exportDataUseCase, activityRepository)
    }

    val restoreFromBackupUseCase: RestoreFromBackupUseCase by lazy {
        RestoreFromBackupUseCase(importDataUseCase, activityRepository)
    }
}
