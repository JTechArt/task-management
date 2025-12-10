# Architecture Overview - Maven/Kotlin Standalone Application

## System Architecture

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│            Standalone Desktop Application (Dockerized)           │
├──────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌────────────────────────┐         ┌──────────────────────────┐ │
│  │   Compose Desktop UI   │         │   Kotlin Backend Logic   │ │
│  │   (View Layer)         │◄────────│   (Domain + Services)    │ │
│  │                        │         │                          │ │
│  │  - Screens             │         │  - Domain Models         │ │
│  │  - Components          │         │  - Use Cases             │ │
│  │  - ViewModels          │         │  - Service Layer         │ │
│  │  - Material Design 3   │         │  - Repository Interface  │ │
│  └────────────────────────┘         └──────────────────────────┘ │
│                                                │                   │
│                                                │                   │
│                          ┌─────────────────────┴────────────┐     │
│                          │                                  │     │
│                   ┌──────▼──────────┐             ┌─────────▼────┐ │
│                   │   PostgreSQL    │             │  External    │ │
│                   │   Database      │             │  Services    │ │
│                   │   (Docker)      │             │              │ │
│                   │                 │             │  - Git (JGit)│ │
│                   │  - Tasks        │             │  - Slack API │ │
│                   │  - Projects     │             │  - Cursor IDE│ │
│                   │  - Rules        │             └──────────────┘ │
│                   └─────────────────┘                              │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Application Layer
- **Kotlin 2.0+** - Modern JVM language
- **Jetpack Compose Desktop 1.6+** - Declarative UI framework
- **Coroutines** - Structured concurrency
- **Maven 3.9+** - Build system

### Data Layer
- **PostgreSQL 16+** - Relational database (Dockerized)
- **Exposed ORM** - Type-safe SQL DSL
- **HikariCP** - Connection pooling
- **Flyway** - Database migrations

### Integration Layer
- **JGit 6.9+** - Pure Java Git implementation
- **Ktor Client** - HTTP client for Slack/APIs
- **kotlinx.serialization** - JSON serialization

### Infrastructure
- **Docker Compose** - Container orchestration
- **JPackage** - Native installer generation

## Architectural Patterns

### 1. Clean Architecture (Layered)

```
┌──────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  ┌────────────────────────────────────────────────┐  │
│  │  Compose Screens, ViewModels, UI State        │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────┐
│                  Domain Layer                        │
│  ┌────────────────────────────────────────────────┐  │
│  │  Use Cases, Domain Models, Repository Interfaces│ │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────┐
│                   Data Layer                         │
│  ┌────────────────────────────────────────────────┐  │
│  │  Repository Impl, Database Entities, DAOs     │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────┐
│              Infrastructure Layer                    │
│  ┌────────────────────────────────────────────────┐  │
│  │  PostgreSQL, Git, External APIs, File System  │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

**Benefits**:
- Clear separation of concerns
- Testable business logic
- Independent of frameworks
- Database agnostic domain layer

### 2. Repository Pattern

Abstraction layer between domain and data access:

```kotlin
// Domain Layer - Interface
interface TaskRepository {
    suspend fun findById(id: UUID): Task?
    suspend fun findAll(): List<Task>
    suspend fun findByProject(projectId: UUID): List<Task>
    suspend fun create(task: Task): Task
    suspend fun update(id: UUID, updates: TaskUpdate): Task
    suspend fun delete(id: UUID)
}

// Data Layer - Implementation
class TaskRepositoryImpl(
    private val database: Database
) : TaskRepository {
    override suspend fun findById(id: UUID): Task? = withContext(Dispatchers.IO) {
        transaction(database) {
            Tasks.select { Tasks.id eq id }
                .singleOrNull()
                ?.toTask()
        }
    }
    
    override suspend fun create(task: Task): Task = withContext(Dispatchers.IO) {
        transaction(database) {
            Tasks.insert {
                it[id] = task.id
                it[title] = task.title
                // ... other fields
            }
            task
        }
    }
}
```

### 3. MVVM (Model-View-ViewModel)

UI architecture pattern for Compose Desktop:

```kotlin
// Model (Domain)
data class Task(
    val id: UUID,
    val title: String,
    val status: TaskStatus
)

// ViewModel
class TaskViewModel(
    private val taskRepository: TaskRepository
) {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskRepository.findAll()
        }
    }
}

// View (Compose)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    
    LazyColumn {
        items(tasks) { task ->
            TaskCard(task = task)
        }
    }
}
```

### 4. Use Case Pattern

Encapsulate business logic in single-purpose use cases:

```kotlin
class CreateTaskUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val gitService: GitService
) {
    suspend operator fun invoke(request: CreateTaskRequest): Result<Task> {
        return try {
            // Validate project exists
            val project = projectRepository.findById(request.projectId)
                ?: return Result.failure(ProjectNotFoundException())
            
            // Clone repository if needed
            if (!gitService.isRepositoryCloned(project.repositoryUrl)) {
                gitService.cloneRepository(
                    url = project.repositoryUrl,
                    destination = project.workspacePath
                )
            }
            
            // Create branch
            val branchName = project.branchTemplate
                .replace("{taskId}", request.taskId)
            gitService.createBranch(project.workspacePath, branchName)
            
            // Create task
            val task = Task(
                id = UUID.randomUUID(),
                title = request.title,
                description = request.description,
                taskType = request.taskType,
                status = TaskStatus.PENDING,
                projectId = project.id,
                workspacePath = "${project.workspacePath}/$branchName",
                branchName = branchName,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            
            val createdTask = taskRepository.create(task)
            Result.success(createdTask)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 5. Dependency Injection (Manual)

Constructor-based dependency injection:

```kotlin
// Composition Root
object DependencyContainer {
    // Database
    private val dataSource: HikariDataSource by lazy {
        createDataSource(loadDatabaseConfig())
    }
    
    private val database: Database by lazy {
        Database.connect(dataSource)
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
        GitServiceImpl()
    }
    
    val slackService: SlackService by lazy {
        SlackServiceImpl(loadSlackConfig())
    }
    
    // Use Cases
    val createTaskUseCase: CreateTaskUseCase by lazy {
        CreateTaskUseCase(taskRepository, projectRepository, gitService)
    }
    
    val deleteTaskUseCase: DeleteTaskUseCase by lazy {
        DeleteTaskUseCase(taskRepository, gitService)
    }
    
    // ViewModels
    fun taskViewModel(): TaskViewModel {
        return TaskViewModel(
            createTaskUseCase,
            deleteTaskUseCase,
            taskRepository
        )
    }
}
```

## Data Flow

### Task Creation Flow

```
User Interface (Compose Desktop)
        │
        │ User fills form and clicks "Create"
        ▼
  TaskCreateScreen.kt
        │
        │ viewModel.createTask(request)
        ▼
  TaskViewModel
        │
        │ launch coroutine
        ▼
  CreateTaskUseCase.invoke(request)
        │
        ├──► ProjectRepository.findById()
        │    ↓
        │    PostgreSQL Query (SELECT FROM projects)
        │    ↓
        │    Return Project
        │
        ├──► GitService.cloneRepository()
        │    ↓
        │    JGit: Git.cloneRepository()
        │    ↓
        │    Clone to workspace directory
        │
        ├──► GitService.createBranch()
        │    ↓
        │    JGit: git.branchCreate()
        │    ↓
        │    Create task-{id} branch
        │
        └──► TaskRepository.create()
             ↓
             PostgreSQL: INSERT INTO tasks
             ↓
             Return created Task
        │
        │ Update StateFlow with new task
        ▼
  TaskListScreen updates automatically
        │
        │ Compose recomposition
        ▼
   UI displays new task card
```

### Database Query Flow

```
UI Request (e.g., Load All Tasks)
        │
        ▼
  TaskViewModel.loadTasks()
        │
        │ viewModelScope.launch(Dispatchers.IO)
        ▼
  TaskRepository.findAll()
        │
        │ withContext(Dispatchers.IO)
        ▼
  Exposed: transaction { ... }
        │
        ▼
  HikariCP Connection Pool
        │
        │ Get connection from pool
        ▼
  PostgreSQL Database
        │
        │ Execute: SELECT * FROM tasks ORDER BY created_at DESC
        ▼
  ResultSet → List<ResultRow>
        │
        │ map { it.toTask() }
        ▼
  List<Task> (Domain Objects)
        │
        │ Return to ViewModel
        ▼
  StateFlow update: _tasks.value = taskList
        │
        │ Compose observes StateFlow
        ▼
  LazyColumn recomposes with new data
```

## Database Schema

### Core Tables

```sql
-- Projects
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL UNIQUE,
    repository_url VARCHAR(500) NOT NULL,
    description TEXT,
    workspace_path VARCHAR(1000) NOT NULL,
    branch_template VARCHAR(200) DEFAULT 'task-{taskId}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    workspace_path VARCHAR(1000),
    branch_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Rules
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    is_global BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Project-Rule Association
CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, rule_id)
);

-- Slack Channels
CREATE TABLE slack_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slack_channel_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_private BOOLEAN DEFAULT FALSE,
    project_id UUID REFERENCES projects(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Indexes for Performance

```sql
-- Task indexes
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created ON tasks(created_at DESC);
CREATE INDEX idx_tasks_type ON tasks(task_type);

-- Project indexes
CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_projects_created ON projects(created_at DESC);

-- Rule indexes
CREATE INDEX idx_rules_category ON rules(category);
CREATE INDEX idx_rules_global ON rules(is_global);

-- Full-text search indexes
CREATE INDEX idx_tasks_search ON tasks USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));
CREATE INDEX idx_projects_search ON projects USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
```

## Concurrency & Asynchronous Operations

### Kotlin Coroutines Architecture

```kotlin
// Structured Concurrency with CoroutineScope
class TaskViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    // Scope tied to ViewModel lifecycle
    private val viewModelScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )
    
    fun loadTasks() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // Repository calls on IO dispatcher
                val tasks = withContext(Dispatchers.IO) {
                    taskRepository.findAll()
                }
                // UI update on Main dispatcher
                _tasks.value = tasks
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
    
    // Parallel operations
    fun loadDashboardData() {
        viewModelScope.launch {
            val tasks = async { taskRepository.findAll() }
            val projects = async { projectRepository.findAll() }
            val stats = async { calculateStatistics() }
            
            // Wait for all to complete
            _dashboardData.value = DashboardData(
                tasks = tasks.await(),
                projects = projects.await(),
                statistics = stats.await()
            )
        }
    }
    
    override fun onCleared() {
        viewModelScope.cancel() // Cancel all coroutines
    }
}
```

### Database Transaction Management

```kotlin
// Transactional repository method
suspend fun transferTaskToProject(
    taskId: UUID,
    newProjectId: UUID
): Task = withContext(Dispatchers.IO) {
    transaction {
        // All operations in single transaction
        val task = Tasks.select { Tasks.id eq taskId }
            .singleOrNull()
            ?.toTask()
            ?: throw TaskNotFoundException(taskId)
        
        val project = Projects.select { Projects.id eq newProjectId }
            .singleOrNull()
            ?.toProject()
            ?: throw ProjectNotFoundException(newProjectId)
        
        // Update task
        Tasks.update({ Tasks.id eq taskId }) {
            it[projectId] = newProjectId
            it[updatedAt] = Instant.now()
        }
        
        // Log transfer
        TaskHistory.insert {
            it[this.taskId] = taskId
            it[action] = "PROJECT_TRANSFER"
            it[metadata] = json {
                "from" to task.projectId
                "to" to newProjectId
            }
        }
        
        task.copy(projectId = newProjectId)
    }
}
```

## Error Handling

### Domain-Level Exceptions

```kotlin
sealed class DomainException(message: String) : Exception(message)

class TaskNotFoundException(taskId: UUID) : 
    DomainException("Task not found: $taskId")

class ProjectNotFoundException(projectId: UUID) : 
    DomainException("Project not found: $projectId")

class RepositoryCloneException(url: String, cause: Throwable) : 
    DomainException("Failed to clone repository: $url - ${cause.message}")

class BranchCreationException(branchName: String, cause: Throwable) : 
    DomainException("Failed to create branch: $branchName - ${cause.message}")
```

### Error Handling in ViewModels

```kotlin
class TaskViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Task>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Task>>> = _uiState.asStateFlow()
    
    fun loadTasks() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val result = runCatching {
                taskRepository.findAll()
            }
            
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### UI Error Display

```kotlin
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is UiState.Loading -> {
            CircularProgressIndicator()
        }
        is UiState.Success -> {
            TaskList(tasks = state.data)
        }
        is UiState.Error -> {
            ErrorDialog(
                message = state.message,
                onRetry = { viewModel.loadTasks() },
                onDismiss = { /* handle */ }
            )
        }
    }
}
```

## Security Architecture

### Database Security

```kotlin
// Connection with SSL
val hikariConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://${host}:${port}/${dbName}?ssl=true&sslmode=require"
    username = System.getenv("DB_USER") ?: throw SecurityException("DB_USER not set")
    password = System.getenv("DB_PASSWORD") ?: throw SecurityException("DB_PASSWORD not set")
}
```

### Credential Storage

```kotlin
object SecureConfig {
    private val encryptionKey by lazy {
        System.getenv("ENCRYPTION_KEY") 
            ?: throw SecurityException("ENCRYPTION_KEY not set")
    }
    
    fun getSlackToken(): String {
        val encrypted = loadFromConfig("slack.token")
        return decrypt(encrypted, encryptionKey)
    }
    
    fun getGitCredentials(url: String): GitCredentials {
        // Use git-credential helper
        return GitCredentialHelper.get(url)
    }
}
```

### Input Validation

```kotlin
object Validator {
    fun validateTaskTitle(title: String): Result<String> {
        return when {
            title.isBlank() -> Result.failure(
                ValidationException("Title cannot be blank")
            )
            title.length > 500 -> Result.failure(
                ValidationException("Title too long (max 500 chars)")
            )
            else -> Result.success(title.trim())
        }
    }
    
    fun validateRepositoryUrl(url: String): Result<String> {
        return try {
            val uri = URI(url)
            when {
                uri.scheme !in listOf("https", "http", "git", "ssh") ->
                    Result.failure(ValidationException("Invalid protocol"))
                else -> Result.success(url)
            }
        } catch (e: Exception) {
            Result.failure(ValidationException("Invalid URL format"))
        }
    }
}
```

## Performance Optimization

### Database Query Optimization

```kotlin
// Use projections to select only needed columns
suspend fun getTaskSummaries(): List<TaskSummary> = withContext(Dispatchers.IO) {
    transaction {
        Tasks
            .slice(Tasks.id, Tasks.title, Tasks.status, Tasks.projectId)
            .selectAll()
            .map { TaskSummary(it[Tasks.id], it[Tasks.title], it[Tasks.status]) }
    }
}

// Batch loading with join
suspend fun getTasksWithProjects(): List<TaskWithProject> = withContext(Dispatchers.IO) {
    transaction {
        (Tasks innerJoin Projects)
            .selectAll()
            .map { row ->
                TaskWithProject(
                    task = row.toTask(),
                    project = row.toProject()
                )
            }
    }
}
```

### UI Performance

```kotlin
// Remember expensive computations
@Composable
fun TaskStatistics(tasks: List<Task>) {
    val statistics = remember(tasks) {
        calculateStatistics(tasks) // Only recompute when tasks change
    }
    
    StatisticsDisplay(statistics)
}

// Lazy loading with pagination
@Composable
fun TaskList(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val lazyListState = rememberLazyListState()
    
    LazyColumn(state = lazyListState) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(task = task)
        }
    }
    
    // Load more when reaching end
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collectLatest { lastIndex ->
                if (lastIndex != null && lastIndex >= tasks.size - 5) {
                    viewModel.loadMoreTasks()
                }
            }
    }
}
```

## Deployment Architecture

### Docker Compose Setup

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

  app:
    build: .
    container_name: aitask-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: aitask
      DB_USER: aitask
      DB_PASSWORD: ${DB_PASSWORD}
    volumes:
      - ./workspaces:/app/workspaces
      - ./config:/app/config
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

### Application Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/aitask-kotlin-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Directory Structure

```
aitask-kotlin/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/aitask/
│   │   │       ├── AitaskApplication.kt
│   │   │       ├── ui/
│   │   │       │   ├── screens/
│   │   │       │   ├── components/
│   │   │       │   ├── theme/
│   │   │       │   └── viewmodel/
│   │   │       ├── domain/
│   │   │       │   ├── model/
│   │   │       │   ├── repository/
│   │   │       │   └── usecase/
│   │   │       ├── data/
│   │   │       │   ├── entity/
│   │   │       │   ├── repository/
│   │   │       │   └── migration/
│   │   │       ├── service/
│   │   │       │   ├── git/
│   │   │       │   ├── slack/
│   │   │       │   └── ide/
│   │   │       ├── config/
│   │   │       └── util/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── logback.xml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__initial_schema.sql
│   │               └── V2__add_indexes.sql
│   └── test/
│       └── kotlin/
└── README.md
```

## Related Documentation

- [Environment Setup](00-environment-setup.md)
- [Technology Stack](01-technology-stack.md)
- [Database Design](03-database-design.md)
- [Docker Setup](05-docker-setup.md)
- [Migration Plan](11-migration-plan.md)
