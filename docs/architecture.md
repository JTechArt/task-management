# AiTask Architecture Documentation

## Document Information

| Property | Value |
|----------|-------|
| Version | 1.1 |
| Date | 2026-03-12 |
| Status | Draft |
| Author | Architecture Team |
| Based On | [PRD v0.1](prd.md) |
| Last Updated | 2026-03-12 - Kotlin 2.1.0 & Compose 1.8.0 upgrade |

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Overview](#system-overview)
3. [Architectural Principles](#architectural-principles)
4. [System Architecture](#system-architecture)
5. [Component Architecture](#component-architecture)
6. [Data Architecture](#data-architecture)
7. [Integration Architecture](#integration-architecture)
8. [Security Architecture](#security-architecture)
9. [Deployment Architecture](#deployment-architecture)
10. [Quality Attributes](#quality-attributes)
11. [Technology Stack](#technology-stack)
12. [Development Guidelines](#development-guidelines)

---

## Executive Summary

AiTask is a cross-platform desktop application designed to streamline developer workflows by automating task workspace creation, repository preparation, branch setup, and IDE launch with AI-assisted development rules. This document defines the architectural blueprint for implementing AiTask as a desktop-first monolith using Kotlin, Compose Multiplatform, and PostgreSQL.

### Key Architectural Decisions

- **Desktop-First Monolith**: Single application with modular internal boundaries
- **Clean Architecture**: Layered design with clear separation of concerns
- **Kotlin/JVM**: Type-safe, modern language with excellent tooling
- **Compose Multiplatform**: Cross-platform UI framework for Windows, macOS, and Linux
- **PostgreSQL**: Robust relational database with ACID compliance
- **Manual Dependency Injection**: Constructor-based DI for simplicity and testability

---

## System Overview

### Purpose

AiTask reduces per-task development environment setup time by automating workspace creation, repository preparation, branch setup, and IDE launch while applying project-specific AI rules automatically.

### Scope

**In Scope:**
- Desktop application for Windows, macOS, and Linux
- Project and multi-repository management
- Task lifecycle management (create, track, complete, archive)
- Git integration (clone, branch, commit, push)
- IDE integration (Cursor, VS Code, JetBrains IDEs)
- Rule management and automatic application
- Slack notifications
- Import/export and backup/restore
- Health monitoring and activity tracking

**Out of Scope:**
- Web-based interface
- Mobile applications
- Real-time collaboration features
- Built-in code editor
- CI/CD pipeline management
- Issue tracker integration (beyond basic task management)

### Stakeholders

- **Primary Users**: Individual developers and small development teams
- **Secondary Users**: Team leads managing project configurations
- **System Administrators**: IT staff managing deployments and backups

---

## Architectural Principles

### 1. Separation of Concerns
Each layer and component has a single, well-defined responsibility. UI logic is separated from business logic, which is separated from data access.

### 2. Dependency Inversion
High-level modules do not depend on low-level modules. Both depend on abstractions (interfaces). This enables testability and flexibility.

### 3. Single Responsibility
Each class, module, and component should have one reason to change. This improves maintainability and reduces coupling.

### 4. Fail-Safe Defaults
The system should default to safe operations. Destructive actions require explicit confirmation. Failed operations should not corrupt data.

### 5. Progressive Disclosure
The UI should present essential information first, with advanced features accessible but not overwhelming. Power users can access deeper configuration.

### 6. Offline-First
Core functionality (browsing projects, tasks, and local data) should work without network connectivity. External integrations degrade gracefully.

### 7. Testability
All business logic should be unit-testable. Integration points should be testable with controlled test doubles.

### 8. Cross-Platform Consistency
The application should provide a consistent experience across Windows, macOS, and Linux while respecting platform conventions.

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    AiTask Desktop Application                       │
│                     (Kotlin + Compose Multiplatform)                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────────────────┐      ┌──────────────────────────┐    │
│  │   Presentation Layer     │      │   Application Layer      │    │
│  │   (Compose Desktop UI)   │◄─────│   (Use Cases/Services)   │    │
│  │                          │      │                          │    │
│  │  • Screens               │      │  • Business Logic        │    │
│  │  • ViewModels            │      │  • Orchestration         │    │
│  │  • UI Components         │      │  • Validation            │    │
│  │  • Theme/Styling         │      │  • Workflow Management   │    │
│  └──────────────────────────┘      └──────────────────────────┘    │
│                                                │                     │
│                                                │                     │
│                          ┌─────────────────────┴──────────────┐     │
│                          │                                     │     │
│                   ┌──────▼──────────┐              ┌──────────▼────┐│
│                   │  Domain Layer   │              │  Data Layer   ││
│                   │                 │              │               ││
│                   │  • Models       │              │  • Repositories││
│                   │  • Interfaces   │              │  • Entities   ││
│                   │  • Value Objects│              │  • DAOs       ││
│                   └─────────────────┘              └───────────────┘│
│                                                            │         │
└────────────────────────────────────────────────────────────┼─────────┘
                                                             │
                    ┌────────────────────────────────────────┼─────────┐
                    │         Infrastructure Layer           │         │
                    ├────────────────────────────────────────┼─────────┤
                    │                                        │         │
                    │  ┌──────────────┐  ┌─────────────┐   │         │
                    │  │  PostgreSQL  │  │  External   │   │         │
                    │  │   Database   │  │  Services   │   │         │
                    │  │              │  │             │   │         │
                    │  │  • Projects  │  │  • Git      │   │         │
                    │  │  • Tasks     │  │  • Slack    │   │         │
                    │  │  • Rules     │  │  • IDEs     │   │         │
                    │  └──────────────┘  └─────────────┘   │         │
                    │                                        │         │
                    └────────────────────────────────────────┴─────────┘
```

### Layered Architecture

AiTask follows a **Clean Architecture** pattern with four distinct layers:

#### 1. Presentation Layer (UI)
- **Technology**: Jetpack Compose for Desktop with Material 3
- **Responsibility**: User interface, user input handling, visual feedback
- **Components**: Screens, ViewModels, UI Components, Theme
- **Communication**: Observes state from ViewModels, dispatches user actions

#### 2. Application Layer (Use Cases)
- **Technology**: Kotlin coroutines for async operations
- **Responsibility**: Business workflows, orchestration, validation
- **Components**: Use Cases, Application Services, DTOs
- **Communication**: Invoked by ViewModels, coordinates Domain and Data layers

#### 3. Domain Layer (Business Logic)
- **Technology**: Pure Kotlin (no framework dependencies)
- **Responsibility**: Core business rules, domain models, repository interfaces
- **Components**: Domain Models, Repository Interfaces, Value Objects, Domain Exceptions
- **Communication**: Used by Application layer, defines contracts for Data layer

#### 4. Data Layer (Persistence & External Services)
- **Technology**: Exposed ORM, HikariCP, JGit, Ktor Client
- **Responsibility**: Data persistence, external service integration
- **Components**: Repository Implementations, Database Entities, Service Clients
- **Communication**: Implements Domain interfaces, accesses infrastructure

#### 5. Infrastructure Layer (External Systems)
- **Technology**: PostgreSQL, Git, Slack API, File System
- **Responsibility**: External systems and resources
- **Components**: Database, Git repositories, External APIs, File system

---

## Component Architecture

### Core Components

#### 1. Project Management Component

**Purpose**: Manage project configurations, repositories, and workspace settings.

**Key Classes:**
```kotlin
// Domain
data class Project(
    val id: UUID,
    val name: String,
    val description: String?,
    val workspacePath: String,
    val branchTemplate: String,
    val repositories: List<Repository>,
    val tags: List<String>,
    val team: String?
)

data class Repository(
    val id: UUID,
    val projectId: UUID,
    val name: String,
    val cloneUrl: String,
    val provider: GitProvider,
    val authType: AuthType,
    val preferredIDEs: List<IDEType>
)

interface ProjectRepository {
    suspend fun findById(id: UUID): Project?
    suspend fun findAll(): List<Project>
    suspend fun create(project: Project): Project
    suspend fun update(id: UUID, updates: ProjectUpdate): Project
    suspend fun archive(id: UUID)
}

// Use Case
class CreateProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val validator: ProjectValidator
) {
    suspend operator fun invoke(request: CreateProjectRequest): Result<Project>
}
```

**Responsibilities:**
- Project CRUD operations
- Repository configuration management
- Workspace path validation
- Branch template management
- Project import/export

#### 2. Task Management Component

**Purpose**: Manage task lifecycle from creation to completion.

**Key Classes:**
```kotlin
// Domain
data class Task(
    val id: UUID,
    val title: String,
    val description: String?,
    val taskType: TaskType,
    val status: TaskStatus,
    val projectId: UUID,
    val workspacePath: String?,
    val branchName: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant?
)

enum class TaskType {
    FEATURE, BUG_FIX, RESEARCH, ENHANCEMENT, DOCUMENTATION, REFACTORING
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, ARCHIVED
}

interface TaskRepository {
    suspend fun findById(id: UUID): Task?
    suspend fun findByProject(projectId: UUID): List<Task>
    suspend fun findByStatus(status: TaskStatus): List<Task>
    suspend fun create(task: Task): Task
    suspend fun updateStatus(id: UUID, status: TaskStatus): Task
    suspend fun delete(id: UUID)
}

// Use Case
class CreateTaskWorkspaceUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val gitService: GitService,
    private val workspaceService: WorkspaceService,
    private val ruleService: RuleService
) {
    suspend operator fun invoke(request: CreateWorkspaceRequest): Result<Workspace>
}
```

**Responsibilities:**
- Task CRUD operations
- Status transition management
- Task filtering and search
- Workspace generation coordination
- Task archival and cleanup

#### 3. Git Integration Component

**Purpose**: Provide Git operations for repository management and branch automation.

**Key Classes:**
```kotlin
// Domain
interface GitService {
    suspend fun cloneRepository(
        url: String,
        destination: String,
        authConfig: GitAuthConfig,
        shallow: Boolean = true
    ): Result<Unit>

    suspend fun createBranch(
        repoPath: String,
        branchName: String,
        baseBranch: String = "main"
    ): Result<String>

    suspend fun validateRepository(url: String): Result<RepositoryInfo>

    suspend fun commitChanges(
        repoPath: String,
        message: String,
        files: List<String>
    ): Result<String>

    suspend fun pushBranch(
        repoPath: String,
        branchName: String,
        remote: String = "origin"
    ): Result<Unit>
}

data class GitAuthConfig(
    val type: AuthType,
    val credentials: GitCredentials?
)

enum class AuthType {
    SSH, HTTPS, TOKEN
}

sealed class GitCredentials {
    data class UsernamePassword(val username: String, val password: String) : GitCredentials()
    data class Token(val token: String) : GitCredentials()
    data class SSHKey(val keyPath: String, val passphrase: String?) : GitCredentials()
}

// Implementation
class JGitService : GitService {
    // JGit-based implementation
}
```

**Responsibilities:**
- Repository cloning (shallow and full)
- Branch creation and management
- Repository validation
- Commit and push operations
- Multi-provider support (GitHub, GitLab, Bitbucket)
- Authentication handling (SSH, HTTPS, tokens)

#### 4. IDE Integration Component

**Purpose**: Launch configured IDEs with task workspaces and applied rules.

**Key Classes:**
```kotlin
// Domain
interface IDEService {
    suspend fun launchIDE(
        ideType: IDEType,
        workspacePath: String,
        taskContext: TaskContext?
    ): Result<Unit>

    suspend fun detectInstalledIDEs(): List<InstalledIDE>

    suspend fun validateIDEPath(ideType: IDEType, path: String): Boolean
}

enum class IDEType {
    CURSOR, VS_CODE, INTELLIJ_IDEA, WEBSTORM, PYCHARM, GOLAND
}

data class InstalledIDE(
    val type: IDEType,
    val version: String,
    val executablePath: String
)

data class TaskContext(
    val taskId: UUID,
    val title: String,
    val description: String?,
    val projectName: String,
    val branchName: String?
)

// Implementation
class DesktopIDEService(
    private val processExecutor: ProcessExecutor,
    private val fileSystem: FileSystem
) : IDEService {
    // Platform-specific IDE launching
}
```

**Responsibilities:**
- IDE detection and validation
- IDE process launching
- Task context file generation
- Cross-platform path resolution
- IDE-specific configuration

#### 5. Rule Management Component

**Purpose**: Manage and apply development rules to task workspaces.

**Key Classes:**
```kotlin
// Domain
data class Rule(
    val id: UUID,
    val name: String,
    val content: String,
    val category: RuleCategory?,
    val scope: RuleScope,
    val targetIDE: IDEType?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class RuleScope {
    GLOBAL, PROJECT, REPOSITORY, IDE
}

enum class RuleCategory {
    CODING_STANDARDS, ARCHITECTURE, TESTING, DOCUMENTATION, AI_ASSISTANT
}

interface RuleRepository {
    suspend fun findById(id: UUID): Rule?
    suspend fun findByScope(scope: RuleScope): List<Rule>
    suspend fun findByProject(projectId: UUID): List<Rule>
    suspend fun create(rule: Rule): Rule
    suspend fun update(id: UUID, updates: RuleUpdate): Rule
    suspend fun delete(id: UUID)
}

// Use Case
class ApplyRulesToWorkspaceUseCase(
    private val ruleRepository: RuleRepository,
    private val fileSystem: FileSystem
) {
    suspend operator fun invoke(
        workspacePath: String,
        projectId: UUID,
        ideType: IDEType
    ): Result<AppliedRules>
}
```

**Responsibilities:**
- Rule CRUD operations
- Rule scope management (global, project, repository, IDE)
- Rule application to workspaces
- Rule conflict resolution
- Rule import/export

#### 6. Workspace Management Component

**Purpose**: Create and manage task-specific workspaces.

**Key Classes:**
```kotlin
// Domain
data class Workspace(
    val taskId: UUID,
    val path: String,
    val repositories: List<WorkspaceRepository>,
    val appliedRules: List<UUID>,
    val createdAt: Instant
)

data class WorkspaceRepository(
    val repositoryId: UUID,
    val localPath: String,
    val branchName: String,
    val cloneStatus: CloneStatus
)

enum class CloneStatus {
    PENDING, CLONING, COMPLETED, FAILED
}

interface WorkspaceService {
    suspend fun createWorkspace(
        task: Task,
        project: Project,
        selectedRepositories: List<Repository>
    ): Result<Workspace>

    suspend fun prepareWorkspace(workspace: Workspace): Result<Unit>

    suspend fun cleanupWorkspace(
        workspacePath: String,
        retentionPolicy: RetentionPolicy
    ): Result<Unit>
}

enum class RetentionPolicy {
    RETAIN, ARCHIVE, DELETE
}
```

**Responsibilities:**
- Workspace directory creation
- Repository cloning coordination
- Branch creation coordination
- Rule application coordination
- Workspace cleanup and archival

#### 7. Integration Component

**Purpose**: Manage external integrations (Slack, OAuth providers).

**Key Classes:**
```kotlin
// Domain
interface SlackService {
    suspend fun sendNotification(
        channelId: String,
        message: SlackMessage
    ): Result<Unit>

    suspend fun validateConnection(): Result<Boolean>
}

data class SlackMessage(
    val text: String,
    val attachments: List<SlackAttachment>? = null,
    val threadTs: String? = null
)

data class SlackChannel(
    val id: UUID,
    val slackChannelId: String,
    val name: String,
    val projectId: UUID?,
    val enabledEvents: Set<TaskEvent>
)

enum class TaskEvent {
    TASK_CREATED, TASK_STARTED, TASK_COMPLETED, WORKSPACE_CREATED, IDE_LAUNCHED
}

interface OAuthService {
    suspend fun initiateOAuthFlow(provider: OAuthProvider): Result<String>
    suspend fun handleCallback(code: String, state: String): Result<OAuthToken>
    suspend fun refreshToken(refreshToken: String): Result<OAuthToken>
}

enum class OAuthProvider {
    SLACK, GITHUB, GITLAB, BITBUCKET
}
```

**Responsibilities:**
- Slack notification delivery
- OAuth authentication flows
- Token management and refresh
- Integration health monitoring
- Event-based notification triggering

#### 8. Activity Tracking Component

**Purpose**: Track and display user actions and system events.

**Key Classes:**
```kotlin
// Domain
data class ActivityEntry(
    val id: UUID,
    val timestamp: Instant,
    val activityType: ActivityType,
    val entityType: EntityType,
    val entityId: UUID,
    val userId: String?,
    val description: String,
    val metadata: Map<String, String>,
    val status: ActivityStatus
)

enum class ActivityType {
    CREATED, UPDATED, DELETED, ARCHIVED,
    WORKSPACE_GENERATED, IDE_LAUNCHED, GIT_OPERATION,
    RULE_APPLIED, NOTIFICATION_SENT
}

enum class EntityType {
    PROJECT, TASK, REPOSITORY, RULE, WORKSPACE
}

enum class ActivityStatus {
    SUCCESS, FAILED, IN_PROGRESS
}

interface ActivityRepository {
    suspend fun create(entry: ActivityEntry): ActivityEntry
    suspend fun findRecent(limit: Int): List<ActivityEntry>
    suspend fun findByEntity(entityType: EntityType, entityId: UUID): List<ActivityEntry>
    suspend fun findByType(activityType: ActivityType): List<ActivityEntry>
}
```

**Responsibilities:**
- Activity logging
- Activity history retrieval
- Activity filtering and search
- Failed operation tracking

---

## Data Architecture

### Database Schema

AiTask uses PostgreSQL 16+ with the following core tables:

#### Entity Relationship Diagram

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│  Projects   │1      * │ Repositories │1      * │    Tasks    │
│─────────────│◄────────│──────────────│◄────────│─────────────│
│ id (PK)     │         │ id (PK)      │         │ id (PK)     │
│ name        │         │ project_id   │         │ title       │
│ description │         │ clone_url    │         │ description │
│ workspace   │         │ provider     │         │ task_type   │
│ branch_tpl  │         │ auth_type    │         │ status      │
│ tags        │         │ ides         │         │ project_id  │
│ team        │         └──────────────┘         │ workspace   │
└─────────────┘                                  │ branch_name │
      │                                          └─────────────┘
      │ *                                              │
      │                                                │
      │         ┌──────────────┐                       │
      └────────►│ProjectRules  │                       │
                │──────────────│                       │
                │ project_id   │                       │
                │ rule_id      │                       │
                └──────────────┘                       │
                      │ *                              │
                      │                                │
                ┌─────▼──────┐                         │
                │   Rules    │                         │
                │────────────│                         │
                │ id (PK)    │                         │
                │ name       │                         │
                │ content    │                         │
                │ category   │                         │
                │ scope      │                         │
                │ target_ide │                         │
                └────────────┘                         │
                                                       │
┌──────────────────┐                                   │
│ SlackChannels    │                                   │
│──────────────────│                                   │
│ id (PK)          │                                   │
│ slack_channel_id │                                   │
│ name             │                                   │
│ project_id (FK)  │                                   │
│ enabled_events   │                                   │
└──────────────────┘                                   │
                                                       │
┌──────────────────┐                                   │
│ ActivityLog      │                                   │
│──────────────────│                                   │
│ id (PK)          │                                   │
│ timestamp        │                                   │
│ activity_type    │                                   │
│ entity_type      │                                   │
│ entity_id        │◄──────────────────────────────────┘
│ description      │
│ status           │
└──────────────────┘
```

#### Core Tables

**1. Projects**
```sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    workspace_path VARCHAR(1000) NOT NULL,
    branch_template VARCHAR(200) DEFAULT 'task-{taskId}',
    tags TEXT[],
    team VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**2. Repositories**
```sql
CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    clone_url VARCHAR(500) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    auth_type VARCHAR(50) NOT NULL,
    preferred_ides TEXT[],
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**3. Tasks**
```sql
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    workspace_path VARCHAR(1000),
    branch_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT valid_task_type CHECK (
        task_type IN ('FEATURE', 'BUG_FIX', 'RESEARCH', 'ENHANCEMENT',
                      'DOCUMENTATION', 'REFACTORING')
    ),
    CONSTRAINT valid_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')
    )
);
```

**4. Rules**
```sql
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    scope VARCHAR(50) NOT NULL,
    target_ide VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_scope CHECK (
        scope IN ('GLOBAL', 'PROJECT', 'REPOSITORY', 'IDE')
    )
);
```

**5. Project Rules (Junction Table)**
```sql
CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, rule_id)
);
```

**6. Slack Channels**
```sql
CREATE TABLE slack_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slack_channel_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    enabled_events TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**7. Activity Log**
```sql
CREATE TABLE activity_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activity_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    user_id VARCHAR(100),
    description TEXT NOT NULL,
    metadata JSONB,
    status VARCHAR(50) NOT NULL
);
```

### Database Indexes

```sql
-- Projects
CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

-- Repositories
CREATE INDEX idx_repositories_project ON repositories(project_id);
CREATE INDEX idx_repositories_provider ON repositories(provider);

-- Tasks
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);

-- Rules
CREATE INDEX idx_rules_scope ON rules(scope);
CREATE INDEX idx_rules_category ON rules(category);

-- Activity Log
CREATE INDEX idx_activity_timestamp ON activity_log(timestamp DESC);
CREATE INDEX idx_activity_entity ON activity_log(entity_type, entity_id);
CREATE INDEX idx_activity_type ON activity_log(activity_type);

-- Full-text search
CREATE INDEX idx_tasks_search ON tasks
    USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));
CREATE INDEX idx_projects_search ON projects
    USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
```

### Data Migration Strategy

**Flyway Migrations:**
- `V1__initial_schema.sql`: Core tables and constraints
- `V2__add_indexes.sql`: Performance indexes
- `V3__add_full_text_search.sql`: Full-text search indexes
- `V4__add_repositories_table.sql`: Multi-repository support
- `V5__add_activity_log.sql`: Activity tracking

**Migration Execution:**
```kotlin
object DatabaseFactory {
    fun migrate(dataSource: HikariDataSource): Int {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .validateOnMigrate(true)
            .load()
            .migrate()
    }
}
```

---

## Integration Architecture

### External System Integrations

#### 1. Git Provider Integration

**Supported Providers:**
- GitHub (github.com, GitHub Enterprise)
- GitLab (gitlab.com, self-hosted)
- Bitbucket (bitbucket.org, Bitbucket Server)
- Generic Git servers

**Integration Pattern:**
```kotlin
interface GitProvider {
    fun parseUrl(url: String): RepositoryInfo
    fun validateUrl(url: String): Boolean
    fun buildCloneUrl(repoInfo: RepositoryInfo, authType: AuthType): String
}

class GitHubProvider : GitProvider {
    override fun parseUrl(url: String): RepositoryInfo {
        // Parse GitHub URLs
    }
}

class GitProviderRegistry {
    private val providers = mapOf(
        "github.com" to GitHubProvider(),
        "gitlab.com" to GitLabProvider(),
        "bitbucket.org" to BitbucketProvider()
    )

    fun detectProvider(url: String): GitProvider {
        // Auto-detect provider from URL
    }
}
```

**Authentication Flow:**
```
1. User configures repository with auth type (SSH/HTTPS/Token)
2. For SSH: Use local SSH config and keys
3. For HTTPS: Store encrypted credentials
4. For Token: Store encrypted personal access token
5. JGit uses configured credentials for operations
```

#### 2. Slack Integration

**Integration Flow:**
```
1. User initiates OAuth flow for Slack
2. Application redirects to Slack OAuth page
3. User authorizes application
4. Slack redirects back with authorization code
5. Application exchanges code for access token
6. Token stored encrypted in database
7. Application sends notifications via Slack API
```

**Notification Triggers:**
```kotlin
enum class TaskEvent {
    TASK_CREATED,
    TASK_STARTED,
    TASK_COMPLETED,
    WORKSPACE_CREATED,
    IDE_LAUNCHED,
    GIT_BRANCH_CREATED
}

class SlackNotificationService(
    private val slackClient: SlackClient,
    private val slackChannelRepository: SlackChannelRepository
) {
    suspend fun notifyTaskEvent(
        taskId: UUID,
        event: TaskEvent,
        task: Task,
        project: Project
    ) {
        val channels = slackChannelRepository.findByProjectAndEvent(
            projectId = task.projectId,
            event = event
        )

        channels.forEach { channel ->
            val message = buildMessage(event, task, project)
            slackClient.sendMessage(channel.slackChannelId, message)
        }
    }
}
```

#### 3. IDE Integration

**Cross-Platform IDE Launching:**
```kotlin
class DesktopIDEService : IDEService {
    override suspend fun launchIDE(
        ideType: IDEType,
        workspacePath: String,
        taskContext: TaskContext?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val idePath = detectIDEPath(ideType)
            ?: return@withContext Result.failure(IDENotFoundException(ideType))

        // Apply rules before launching
        taskContext?.let { applyTaskContext(workspacePath, it) }

        // Launch IDE process
        val command = buildLaunchCommand(ideType, idePath, workspacePath)
        processExecutor.execute(command, detached = true)

        Result.success(Unit)
    }

    private fun detectIDEPath(ideType: IDEType): String? {
        return when (Platform.current) {
            Platform.MACOS -> detectMacOSPath(ideType)
            Platform.WINDOWS -> detectWindowsPath(ideType)
            Platform.LINUX -> detectLinuxPath(ideType)
        }
    }
}
```

**IDE-Specific Paths:**
```kotlin
object IDEPaths {
    val CURSOR_PATHS = mapOf(
        Platform.MACOS to listOf(
            "/Applications/Cursor.app/Contents/MacOS/Cursor"
        ),
        Platform.WINDOWS to listOf(
            "C:\\Users\\{user}\\AppData\\Local\\Programs\\cursor\\Cursor.exe"
        ),
        Platform.LINUX to listOf(
            "/usr/bin/cursor",
            "/usr/local/bin/cursor"
        )
    )

    val VSCODE_PATHS = mapOf(
        Platform.MACOS to listOf(
            "/Applications/Visual Studio Code.app/Contents/MacOS/Electron"
        ),
        Platform.WINDOWS to listOf(
            "C:\\Program Files\\Microsoft VS Code\\Code.exe"
        ),
        Platform.LINUX to listOf(
            "/usr/bin/code",
            "/usr/local/bin/code"
        )
    )
}
```

#### 4. File System Integration

**Workspace Structure:**
```
{workspace_base_path}/
├── {project_name}/
│   ├── task-{task_id}/
│   │   ├── {repository_1}/
│   │   │   ├── .git/
│   │   │   ├── .cursor/
│   │   │   │   └── rules/
│   │   │   │       ├── global-rules.md
│   │   │   │       ├── project-rules.md
│   │   │   │       └── ide-rules.md
│   │   │   ├── task-context.md
│   │   │   └── [repository files]
│   │   ├── {repository_2}/
│   │   │   └── ...
│   │   └── workspace-metadata.json
```

**Workspace Metadata:**
```json
{
  "taskId": "uuid",
  "projectId": "uuid",
  "createdAt": "2026-03-11T10:00:00Z",
  "repositories": [
    {
      "repositoryId": "uuid",
      "localPath": "./repository_1",
      "branchName": "task-123",
      "cloneStatus": "COMPLETED"
    }
  ],
  "appliedRules": ["rule-uuid-1", "rule-uuid-2"]
}
```

---

## Security Architecture

### Security Principles

1. **Defense in Depth**: Multiple layers of security controls
2. **Least Privilege**: Minimal permissions for operations
3. **Secure by Default**: Safe defaults for all configurations
4. **Encryption at Rest**: Sensitive data encrypted in database
5. **No Secrets in Logs**: Credentials never logged or exposed

### Credential Management

**Encryption Strategy:**
```kotlin
interface CredentialStore {
    suspend fun storeCredential(key: String, value: String): Result<Unit>
    suspend fun retrieveCredential(key: String): Result<String>
    suspend fun deleteCredential(key: String): Result<Unit>
}

class EncryptedCredentialStore(
    private val encryptionService: EncryptionService,
    private val database: Database
) : CredentialStore {
    override suspend fun storeCredential(key: String, value: String): Result<Unit> {
        val encrypted = encryptionService.encrypt(value)
        // Store encrypted value in database
        return Result.success(Unit)
    }
}

interface EncryptionService {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}

class AESEncryptionService(
    private val keyProvider: EncryptionKeyProvider
) : EncryptionService {
    // AES-256-GCM encryption
}
```

**Credential Types:**
- Git HTTPS credentials (username/password)
- Git personal access tokens
- SSH key passphrases (optional)
- Slack OAuth tokens
- OAuth refresh tokens

### Authentication & Authorization

**Local User Context:**
```kotlin
data class UserContext(
    val userId: String,
    val username: String,
    val permissions: Set<Permission>
)

enum class Permission {
    CREATE_PROJECT,
    DELETE_PROJECT,
    CREATE_TASK,
    DELETE_TASK,
    MANAGE_RULES,
    CONFIGURE_INTEGRATIONS,
    EXPORT_DATA,
    IMPORT_DATA
}
```

**Note**: MVP focuses on single-user desktop application. Multi-user support is out of scope but architecture allows future extension.

### Data Protection

**Sensitive Data Handling:**
```kotlin
data class Project(
    val id: UUID,
    val name: String,
    // ... other fields
) {
    fun toExportFormat(includeSensitive: Boolean = false): ProjectExport {
        return ProjectExport(
            id = id,
            name = name,
            // Exclude sensitive data unless explicitly requested
            credentials = if (includeSensitive) credentials else null
        )
    }
}
```

**Export Security:**
- Credentials excluded from exports by default
- User must explicitly opt-in to include sensitive data
- Exported files should be encrypted if containing credentials
- Import validates and sanitizes all input data

### Input Validation

**Validation Strategy:**
```kotlin
interface Validator<T> {
    fun validate(input: T): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>
)

data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)

class ProjectValidator : Validator<CreateProjectRequest> {
    override fun validate(input: CreateProjectRequest): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (input.name.isBlank()) {
            errors.add(ValidationError("name", "Name is required", "REQUIRED"))
        }

        if (input.name.length > 200) {
            errors.add(ValidationError("name", "Name too long", "MAX_LENGTH"))
        }

        if (!isValidWorkspacePath(input.workspacePath)) {
            errors.add(ValidationError("workspacePath", "Invalid path", "INVALID"))
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
```

---

## Deployment Architecture

### Desktop Application Packaging

**Target Platforms:**
- Windows 10/11 (x64)
- macOS 11+ (Intel and Apple Silicon)
- Linux (Ubuntu 20.04+, Fedora 35+)

**Packaging Strategy:**
```
┌─────────────────────────────────────────────────────────────┐
│                   Build Process                             │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Gradle Build                                             │
│     ./gradlew clean build                                    │
│     ↓                                                        │
│  2. JPackage Native Installer                                │
│     jpackage --input build/libs/                             │
│              --main-jar desktop-app.jar                      │
│              --type {dmg|exe|deb|rpm}                        │
│     ↓                                                        │
│  3. Platform-Specific Installer                              │
│     • Windows: .exe (MSI installer)                          │
│     • macOS: .dmg (disk image)                               │
│     • Linux: .deb, .rpm packages                             │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

**JPackage Configuration:**
```bash
#!/bin/bash
# build-jpackage.sh

APP_VERSION="0.1.0"
APP_NAME="AiTask"
MAIN_JAR="desktop-app-${APP_VERSION}.jar"

jpackage \
  --input desktop-app/target \
  --name "${APP_NAME}" \
  --main-jar "${MAIN_JAR}" \
  --main-class com.aitask.desktop.TaskManagerAppKt \
  --type dmg \
  --app-version "${APP_VERSION}" \
  --vendor "AiTask" \
  --copyright "Copyright © 2026 AiTask" \
  --description "AI-assisted task workspace manager" \
  --icon resources/icon.icns \
  --java-options "-Xmx2g" \
  --java-options "-Dfile.encoding=UTF-8"
```

### Database Deployment

**PostgreSQL Setup:**

**Option 1: Docker Compose (Development)**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aitask"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

**Option 2: Local PostgreSQL (Production)**
- User installs PostgreSQL separately
- Application connects to local instance
- Flyway migrations run on first launch

**Database Configuration:**
```kotlin
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 5432,
    val name: String = "aitask",
    val user: String = "aitask",
    val password: String,
    val pool: PoolConfig
)

// Environment-based configuration
class EnvConfigLoader : ConfigLoader {
    override fun loadAppConfig(): AppConfig {
        return AppConfig(
            appName = env("APP_NAME", "AiTask"),
            database = DatabaseConfig(
                host = env("DB_HOST", "localhost"),
                port = env("DB_PORT", "5432").toInt(),
                name = env("DB_NAME", "aitask"),
                user = env("DB_USER", "aitask"),
                password = env("DB_PASSWORD") ?: throw ConfigException("DB_PASSWORD required")
            )
        )
    }
}
```

### Application Startup Flow

```
Application Launch
    ↓
┌──────────────────────────────────────┐
│ 1. Load Configuration                │
│    • Environment variables           │
│    • Config files                    │
│    • Default values                  │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│ 2. Initialize Database               │
│    • Create connection pool          │
│    • Run Flyway migrations           │
│    • Verify connectivity             │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│ 3. Initialize Services               │
│    • Dependency injection setup      │
│    • Service initialization          │
│    • Health checks                   │
└──────────────────────────────────────┘
    ↓
┌──────────────────────────────────────┐
│ 4. Launch UI                         │
│    • Compose Desktop window          │
│    • Load initial screen             │
│    • Start background tasks          │
└──────────────────────────────────────┘
```

**Startup Implementation:**
```kotlin
class StartupBootstrapper {
    fun prepareInfrastructure(
        config: AppConfig,
        enableDatabase: Boolean
    ): BootstrapResult {
        if (!enableDatabase) {
            return BootstrapResult(databaseInitialized = false)
        }

        // Create connection pool
        val dataSource = DatabaseFactory.createDataSource(config.database)

        // Run migrations
        val migrationsApplied = DatabaseFactory.migrate(dataSource)
        logger.info { "Applied $migrationsApplied migrations" }

        // Connect Exposed
        DatabaseFactory.connect(dataSource)

        return BootstrapResult(databaseInitialized = true)
    }
}
```

### Distribution Strategy

**Release Channels:**
1. **Stable**: Production-ready releases
2. **Beta**: Feature-complete, testing phase
3. **Alpha**: Early access, experimental features

**Update Mechanism:**
- Manual download and install (MVP)
- Future: Auto-update capability

**Installation Locations:**
- **Windows**: `C:\Program Files\AiTask\`
- **macOS**: `/Applications/AiTask.app`
- **Linux**: `/opt/aitask/` or `/usr/local/bin/aitask`

**User Data Locations:**
- **Windows**: `%APPDATA%\AiTask\`
- **macOS**: `~/Library/Application Support/AiTask/`
- **Linux**: `~/.config/aitask/`

---

## Quality Attributes

### Performance

**Target Metrics:**
- **Startup Time**: < 5 seconds (cold start)
- **Task Workspace Creation**: < 60 seconds (shallow clone)
- **UI Responsiveness**: < 100ms for user interactions
- **Database Queries**: < 200ms for typical queries
- **IDE Launch**: < 3 seconds

**Performance Strategies:**
1. **Lazy Loading**: Load data on-demand
2. **Connection Pooling**: HikariCP with optimized pool size
3. **Async Operations**: Coroutines for long-running tasks
4. **Shallow Clones**: Git shallow clone for faster repository retrieval
5. **Indexed Queries**: Comprehensive database indexing
6. **Caching**: In-memory caching for frequently accessed data

### Scalability

**Capacity Targets:**
- **Projects**: 100+ projects per installation
- **Tasks**: 1,000+ tasks per project
- **Repositories**: 10+ repositories per project
- **Rules**: 100+ rules across all scopes
- **Concurrent Operations**: 5+ parallel workspace creations

**Scalability Strategies:**
1. **Efficient Queries**: Use projections and joins
2. **Pagination**: Lazy loading for large lists
3. **Background Processing**: Async task execution
4. **Resource Cleanup**: Automatic workspace cleanup

### Reliability

**Availability Target**: 99.9% uptime for local operations

**Reliability Strategies:**
1. **Transaction Management**: ACID-compliant database operations
2. **Error Recovery**: Retry logic for transient failures
3. **Data Validation**: Input validation at all boundaries
4. **Graceful Degradation**: Offline mode for local operations
5. **Backup/Restore**: User-initiated backup and restore

**Error Handling:**
```kotlin
sealed class DomainException(message: String) : Exception(message)

class TaskNotFoundException(taskId: UUID) :
    DomainException("Task not found: $taskId")

class RepositoryCloneException(url: String, cause: Throwable) :
    DomainException("Failed to clone repository: $url", cause)

class WorkspaceCreationException(reason: String) :
    DomainException("Workspace creation failed: $reason")
```

### Maintainability

**Code Quality Targets:**
- **Test Coverage**: > 80% for domain and application layers
- **Cyclomatic Complexity**: < 10 per method
- **Code Duplication**: < 5%
- **Documentation**: All public APIs documented

**Maintainability Strategies:**
1. **Clean Architecture**: Clear separation of concerns
2. **SOLID Principles**: Applied throughout codebase
3. **Automated Testing**: Unit and integration tests
4. **Code Reviews**: Required for all changes
5. **Continuous Integration**: Automated builds and tests

### Usability

**Usability Goals:**
- **Learnability**: New users productive within 15 minutes
- **Efficiency**: Common tasks completable in < 5 clicks
- **Error Prevention**: Validation and confirmation for destructive actions
- **Accessibility**: WCAG AA compliance for desktop

**Usability Strategies:**
1. **Consistent UI**: Material 3 design system
2. **Clear Feedback**: Progress indicators for long operations
3. **Helpful Errors**: Actionable error messages
4. **Keyboard Navigation**: Full keyboard support
5. **Contextual Help**: Tooltips and inline guidance

### Security

**Security Goals:**
- **Data Protection**: Credentials encrypted at rest
- **Input Validation**: All user input validated
- **Secure Defaults**: Safe default configurations
- **Audit Trail**: Activity logging for key operations

**Security Strategies:**
1. **Encryption**: AES-256 for sensitive data
2. **Validation**: Input validation at all boundaries
3. **Least Privilege**: Minimal file system permissions
4. **No Secrets in Logs**: Credentials never logged
5. **Secure Export**: Optional encryption for exports

---

## Technology Stack

### Core Technologies

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| **Language** | Kotlin | 2.1.0 | Primary development language with K2 compiler |
| **Runtime** | JDK | 21 | Java Virtual Machine |
| **Build Tool** | Gradle | 8.5+ | Build automation and dependency management with superior Kotlin DSL support |
| **UI Framework** | Compose Multiplatform | 1.8.0 | Cross-platform desktop UI |
| **Database** | PostgreSQL | 16+ | Relational database |
| **ORM** | Exposed | 0.50+ | Type-safe SQL DSL |
| **Connection Pool** | HikariCP | 5.1+ | Database connection pooling |
| **Migrations** | Flyway | 10+ | Database schema migrations |
| **Git Library** | JGit | 6.9+ | Pure Java Git implementation |
| **HTTP Client** | Ktor Client | 2.3+ | HTTP client for APIs |
| **Serialization** | kotlinx.serialization | 1.6+ | JSON serialization |
| **Logging** | Logback + kotlin-logging | 1.4+ / 3.0+ | Structured logging |
| **Testing** | JUnit 5 + Mockk | 5.10+ / 1.13+ | Unit and integration testing |
| **Coroutines** | kotlinx.coroutines | 1.8.1 | Async/concurrent programming |

### Technology Justification

**Kotlin 2.1.0:**
- Modern, concise syntax
- Null safety built-in
- K2 compiler with improved performance and stability
- Excellent IDE support
- Seamless Java interop
- Strong coroutines support
- Built-in Compose compiler (no separate plugin needed)

**Compose Multiplatform 1.8.0:**
- Single codebase for Windows, macOS, Linux
- Declarative UI paradigm
- Material 3 design system
- Active development by JetBrains
- Growing ecosystem
- Fully compatible with Kotlin 2.1.0 and K2 compiler
- Improved rendering and composition performance

**PostgreSQL:**
- ACID compliance
- Robust feature set
- Excellent performance
- Full-text search
- JSON support (JSONB)
- Wide platform support

**JGit:**
- Pure Java implementation
- No external Git binary required
- Cross-platform consistency
- Programmatic Git operations
- Active maintenance

**Exposed ORM:**
- Type-safe SQL DSL
- Kotlin-first design
- Lightweight
- Flexible (DSL and DAO)
- Good PostgreSQL support

### Development Tools

| Tool | Purpose |
|------|---------|
| **IntelliJ IDEA** | Primary IDE |
| **Docker Desktop** | Local PostgreSQL |
| **Git** | Version control |
| **Postman** | API testing (Slack, OAuth) |
| **pgAdmin** | Database management |
| **JPackage** | Native installer creation |

### Third-Party Libraries

**Core Dependencies:**
```kotlin
dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Database
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:10.10.0")

    // Git
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

    // HTTP Client
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
```

---

## Development Guidelines

### Project Structure

```
taskmanager/
├── build.gradle.kts                 # Root Gradle configuration
├── settings.gradle.kts              # Gradle settings
├── gradle.properties                # Gradle properties
├── docker-compose.yml               # PostgreSQL container
├── Dockerfile                       # Application container
├── core/                            # Core module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   │   └── com/aitask/core/
│       │   │       ├── config/      # Configuration
│       │   │       ├── db/          # Database factory
│       │   │       └── logging/     # Logging utilities
│       │   └── resources/
│       │       └── db/
│       │           └── migration/   # Flyway migrations
│       └── test/
│           └── kotlin/
├── desktop-app/                     # Desktop application module
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── kotlin/
│       │   │   └── com/aitask/desktop/
│       │   │       ├── TaskManagerApp.kt    # Main entry point
│       │   │       ├── ui/                  # UI layer
│       │   │       │   ├── screens/         # Screens
│       │   │       │   ├── components/      # Reusable components
│       │   │       │   ├── theme/           # Theme and styling
│       │   │       │   └── viewmodel/       # ViewModels
│       │   │       ├── domain/              # Domain layer
│       │   │       │   ├── model/           # Domain models
│       │   │       │   ├── repository/      # Repository interfaces
│       │   │       │   └── usecase/         # Use cases
│       │   │       ├── data/                # Data layer
│       │   │       │   ├── entity/          # Database entities
│       │   │       │   ├── repository/      # Repository implementations
│       │   │       │   └── mapper/          # Entity-Domain mappers
│       │   │       ├── service/             # External services
│       │   │       │   ├── git/             # Git integration
│       │   │       │   ├── slack/           # Slack integration
│       │   │       │   ├── ide/             # IDE integration
│       │   │       │   └── workspace/       # Workspace management
│       │   │       ├── di/                  # Dependency injection
│       │   │       └── util/                # Utilities
│       │   └── resources/
│       │       ├── logback.xml              # Logging configuration
│       │       └── icon.png                 # Application icon
│       └── test/
│           └── kotlin/
└── docs/                            # Documentation
    ├── architecture.md
    ├── prd.md
    └── ...
```

### Coding Standards

**Kotlin Style Guide:**
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer immutability (val over var)
- Use data classes for DTOs and domain models
- Leverage Kotlin's null safety

**Example:**
```kotlin
// Good
data class Task(
    val id: UUID,
    val title: String,
    val status: TaskStatus
)

suspend fun findTaskById(id: UUID): Task? {
    return taskRepository.findById(id)
}

// Avoid
var task: Task? = null
fun getTask(id: String): Task {
    // ...
}
```

**Naming Conventions:**
- **Classes**: PascalCase (e.g., `TaskRepository`, `CreateTaskUseCase`)
- **Functions**: camelCase (e.g., `findById`, `createWorkspace`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`)
- **Packages**: lowercase (e.g., `com.aitask.domain.model`)

### Testing Strategy

**Test Pyramid:**
```
        ┌─────────────┐
        │   E2E Tests │  (Few - Manual)
        └─────────────┘
      ┌───────────────────┐
      │ Integration Tests │  (Some - Automated)
      └───────────────────┘
    ┌───────────────────────┐
    │     Unit Tests        │  (Many - Automated)
    └───────────────────────┘
```

**Unit Testing:**
```kotlin
class CreateTaskUseCaseTest {
    private val taskRepository = mockk<TaskRepository>()
    private val projectRepository = mockk<ProjectRepository>()
    private val validator = mockk<TaskValidator>()

    private val useCase = CreateTaskUseCase(
        taskRepository,
        projectRepository,
        validator
    )

    @Test
    fun `should create task when valid request`() = runTest {
        // Given
        val request = CreateTaskRequest(
            title = "Test Task",
            projectId = UUID.randomUUID()
        )
        val project = mockk<Project>()

        coEvery { projectRepository.findById(any()) } returns project
        coEvery { validator.validate(any()) } returns ValidationResult.success()
        coEvery { taskRepository.create(any()) } returns mockk()

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess)
        coVerify { taskRepository.create(any()) }
    }
}
```

**Integration Testing:**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskRepositoryIntegrationTest {
    private lateinit var database: Database
    private lateinit var repository: TaskRepository

    @BeforeAll
    fun setup() {
        // Setup test database
        database = setupTestDatabase()
        repository = TaskRepositoryImpl(database)
    }

    @Test
    fun `should persist and retrieve task`() = runTest {
        // Given
        val task = Task(
            id = UUID.randomUUID(),
            title = "Integration Test Task",
            // ...
        )

        // When
        repository.create(task)
        val retrieved = repository.findById(task.id)

        // Then
        assertEquals(task.title, retrieved?.title)
    }
}
```

### Error Handling Patterns

**Result Type:**
```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: DomainException) : Result<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
}

// Usage
suspend fun createTask(request: CreateTaskRequest): Result<Task> {
    return try {
        val task = // ... create task
        Result.Success(task)
    } catch (e: DomainException) {
        Result.Failure(e)
    }
}
```

### Logging Guidelines

**Structured Logging:**
```kotlin
private val logger = KotlinLogging.logger {}

class TaskService {
    suspend fun createTask(request: CreateTaskRequest): Result<Task> {
        logger.info { "Creating task: title=${request.title}, projectId=${request.projectId}" }

        return try {
            val task = // ... create task
            logger.info { "Task created successfully: taskId=${task.id}" }
            Result.Success(task)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create task: ${e.message}" }
            Result.Failure(TaskCreationException(e))
        }
    }
}
```

**Log Levels:**
- **ERROR**: Errors that require attention
- **WARN**: Warnings that don't prevent operation
- **INFO**: Important business events
- **DEBUG**: Detailed diagnostic information
- **TRACE**: Very detailed diagnostic information

### Dependency Injection

**Manual DI Container:**
```kotlin
object DependencyContainer {
    // Database
    private val dataSource: HikariDataSource by lazy {
        DatabaseFactory.createDataSource(loadDatabaseConfig())
    }

    private val database: Database by lazy {
        DatabaseFactory.connect(dataSource)
    }

    // Repositories
    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(database)
    }

    val projectRepository: ProjectRepository by lazy {
        ProjectRepositoryImpl(database)
    }

    // Services
    val gitService: GitService by lazy {
        JGitService()
    }

    val ideService: IDEService by lazy {
        DesktopIDEService(ProcessExecutor(), FileSystem())
    }

    // Use Cases
    val createTaskUseCase: CreateTaskUseCase by lazy {
        CreateTaskUseCase(taskRepository, projectRepository, TaskValidator())
    }

    val createWorkspaceUseCase: CreateTaskWorkspaceUseCase by lazy {
        CreateTaskWorkspaceUseCase(
            taskRepository,
            projectRepository,
            gitService,
            WorkspaceService(),
            RuleService()
        )
    }

    // ViewModels
    fun taskViewModel(): TaskViewModel {
        return TaskViewModel(
            createTaskUseCase,
            taskRepository
        )
    }
}
```

---

## Appendix

### Glossary

| Term | Definition |
|------|------------|
| **Task** | A unit of work with associated workspace and Git branch |
| **Project** | A collection of repositories and configuration for related tasks |
| **Repository** | A Git repository associated with a project |
| **Workspace** | A local directory containing cloned repositories for a task |
| **Rule** | Development guideline or AI assistant instruction |
| **IDE** | Integrated Development Environment (Cursor, VS Code, etc.) |
| **Branch Template** | Pattern for generating Git branch names (e.g., `task-{taskId}`) |
| **Shallow Clone** | Git clone with limited history for faster operations |

### References

- [Product Requirements Document](prd.md)
- [Technology Stack](../tech-appoach/01-technology-stack.md)
- [Database Design](../tech-appoach/03-database-design.md)
- [Architecture Overview](../tech-appoach/14-architecture-overview.md)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/lp/compose-multiplatform/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [JGit Documentation](https://www.eclipse.org/jgit/)

### Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-03-11 | Architecture Team | Initial architecture documentation |

---

**End of Architecture Documentation**

