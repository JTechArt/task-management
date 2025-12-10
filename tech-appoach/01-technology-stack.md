# Technology Stack - Maven/Kotlin Standalone Application

## Overview
Technology choices for implementing AiTask as a standalone application using Maven, Kotlin, Jetpack Compose Desktop, and PostgreSQL. This stack is optimized for public deployment with Docker containerization.

## Build System

### Maven 3.9+
**Purpose**: Build automation and dependency management

**Why Maven**:
- Industry-standard Java/Kotlin build tool
- Central Repository for dependency management
- Extensive plugin ecosystem
- IDE integration (IntelliJ IDEA, VS Code, Eclipse)
- Reproducible builds across environments
- Better enterprise support than Gradle

**pom.xml Configuration**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.aitask</groupId>
    <artifactId>aitask-kotlin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    
    <name>AiTask Desktop Application</name>
    <description>Standalone task management application with Git and Slack integration</description>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <kotlin.version>2.0.0</kotlin.version>
        <kotlin.compiler.jvmTarget>21</kotlin.compiler.jvmTarget>
        <compose.version>1.6.0</compose.version>
        <exposed.version>0.50.0</exposed.version>
        <postgresql.version>42.7.3</postgresql.version>
        <hikari.version>5.1.0</hikari.version>
        <flyway.version>10.10.0</flyway.version>
        <ktor.version>2.3.9</ktor.version>
        <coroutines.version>1.8.0</coroutines.version>
        <jgit.version>6.9.0.202403050737-r</jgit.version>
        <slf4j.version>2.0.12</slf4j.version>
        <logback.version>1.5.3</logback.version>
    </properties>
    
    <dependencies>
        <!-- Kotlin Standard Library -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>
        
        <!-- Compose Desktop -->
        <dependency>
            <groupId>org.jetbrains.compose.desktop</groupId>
            <artifactId>desktop-jvm</artifactId>
            <version>${compose.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.compose.material3</groupId>
            <artifactId>material3-desktop</artifactId>
            <version>${compose.version}</version>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-core</artifactId>
            <version>${exposed.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-dao</artifactId>
            <version>${exposed.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-jdbc</artifactId>
            <version>${exposed.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-java-time</artifactId>
            <version>${exposed.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>${hikari.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        
        <!-- Coroutines -->
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-core-jvm</artifactId>
            <version>${coroutines.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-swing</artifactId>
            <version>${coroutines.version}</version>
        </dependency>
        
        <!-- HTTP Client -->
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-core-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-cio-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-content-negotiation</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-serialization-kotlinx-json</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        
        <!-- Git Integration -->
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>${jgit.version}</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>
        
        <!-- Serialization -->
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-serialization-json</artifactId>
            <version>1.6.3</version>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-test-junit5</artifactId>
            <version>${kotlin.version}</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>io.mockk</groupId>
            <artifactId>mockk-jvm</artifactId>
            <version>1.13.10</version>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-test</artifactId>
            <version>${coroutines.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <sourceDirectory>src/main/kotlin</sourceDirectory>
        <testSourceDirectory>src/test/kotlin</testSourceDirectory>
        
        <plugins>
            <!-- Kotlin Compiler -->
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>test-compile</id>
                        <goals>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <args>
                        <arg>-Xjsr305=strict</arg>
                    </args>
                    <compilerPlugins>
                        <plugin>serialization</plugin>
                    </compilerPlugins>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-serialization</artifactId>
                        <version>${kotlin.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
            
            <!-- Compose Desktop Plugin -->
            <plugin>
                <groupId>org.jetbrains.compose</groupId>
                <artifactId>compose-maven-plugin</artifactId>
                <version>${compose.version}</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>package</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.aitask.AitaskApplicationKt</mainClass>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Flyway Migration Plugin -->
            <plugin>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-maven-plugin</artifactId>
                <version>${flyway.version}</version>
                <configuration>
                    <url>jdbc:postgresql://localhost:5432/aitask</url>
                    <user>aitask</user>
                    <password>aitask_secret</password>
                    <schemas>
                        <schema>public</schema>
                    </schemas>
                    <locations>
                        <location>filesystem:src/main/resources/db/migration</location>
                    </locations>
                </configuration>
            </plugin>
            
            <!-- Fat JAR Creation -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.7.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.aitask.AitaskApplicationKt</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            
            <!-- JPackage Native Installer -->
            <plugin>
                <groupId>org.panteleyev</groupId>
                <artifactId>jpackage-maven-plugin</artifactId>
                <version>1.6.0</version>
                <configuration>
                    <name>AiTask</name>
                    <appVersion>1.0.0</appVersion>
                    <vendor>AiTask</vendor>
                    <destination>target/dist</destination>
                    <module>com.aitask/com.aitask.AitaskApplicationKt</module>
                    <runtimeImage>target/image</runtimeImage>
                    <icon>${project.basedir}/src/main/resources/icon.${os.type}</icon>
                    <javaOptions>
                        <option>-Dfile.encoding=UTF-8</option>
                        <option>-Xms256m</option>
                        <option>-Xmx1024m</option>
                    </javaOptions>
                </configuration>
            </plugin>
            
            <!-- Testing -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
            
            <!-- Code Coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

**Maven Commands**:
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run application
mvn exec:java -Dexec.mainClass="com.aitask.AitaskApplicationKt"

# Database migrations
mvn flyway:migrate
mvn flyway:info
mvn flyway:clean

# Create native installer
mvn package jpackage:jpackage
```

## Application Stack

### Kotlin 2.0+
**Purpose**: Primary application language

**Why Kotlin**:
- **Null Safety**: Compile-time null checking eliminates NullPointerExceptions
- **Concise Syntax**: 30-40% less boilerplate than Java
- **Coroutines**: Native structured concurrency for async operations
- **Data Classes**: Auto-generated equals, hashCode, toString, copy
- **Extension Functions**: Add methods to existing classes without inheritance
- **Smart Casts**: Automatic type casting after type checks
- **JVM Interop**: Full access to Java ecosystem

**Example Code**:
```kotlin
// Domain Model with Data Class
data class Task(
    val id: UUID,
    val title: String,
    val description: String?,
    val taskType: TaskType,
    val status: TaskStatus,
    val projectId: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
) {
    fun isComplete(): Boolean = status == TaskStatus.COMPLETED
    
    fun canBeDeleted(): Boolean = 
        status != TaskStatus.IN_PROGRESS && 
        completedAt == null
}

enum class TaskType {
    FEATURE, BUG_FIX, REFACTOR, DOCUMENTATION, TESTING
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}

// Repository with Coroutines
interface TaskRepository {
    suspend fun findById(id: UUID): Task?
    suspend fun findAll(): List<Task>
    suspend fun findByProject(projectId: UUID): List<Task>
    suspend fun create(task: Task): Task
    suspend fun update(id: UUID, updates: TaskUpdate): Task
    suspend fun delete(id: UUID)
}

// Service with Business Logic
class TaskService(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val gitService: GitService
) {
    suspend fun createTask(request: CreateTaskRequest): Task {
        // Validate project exists
        val project = projectRepository.findById(request.projectId)
            ?: throw ProjectNotFoundException(request.projectId)
        
        // Clone repository if needed
        if (!gitService.repositoryExists(project.repositoryUrl)) {
            gitService.cloneRepository(project.repositoryUrl, project.workspacePath)
        }
        
        // Create branch for task
        val branchName = project.branchTemplate.replace("{taskId}", request.taskId)
        gitService.createBranch(project.workspacePath, branchName)
        
        // Create task entity
        val task = Task(
            id = UUID.randomUUID(),
            title = request.title,
            description = request.description,
            taskType = request.taskType,
            status = TaskStatus.PENDING,
            projectId = project.id,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        return taskRepository.create(task)
    }
}
```

### Jetpack Compose Desktop 1.6+
**Purpose**: Declarative UI framework

**Why Compose Desktop**:
- **Declarative UI**: React-like programming model
- **Cross-Platform**: Windows, macOS, Linux from single codebase
- **Native Performance**: Compiled to native code, no web view
- **Hot Reload**: Instant UI updates during development
- **Material Design 3**: Modern, beautiful UI components
- **Type-Safe**: Compile-time UI validation

**Example UI Code**:
```kotlin
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { viewModel.refreshTasks() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateTaskDialog() }
            ) {
                Icon(Icons.Default.Add, "Create Task")
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            // Task List
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isSelected = task.id == selectedTask?.id,
                        onClick = { viewModel.selectTask(task) },
                        onComplete = { viewModel.completeTask(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
            
            // Task Details Panel
            if (selectedTask != null) {
                TaskDetailsPanel(
                    task = selectedTask!!,
                    onClose = { viewModel.clearSelection() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    isSelected: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    task.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Status Badge
                TaskStatusBadge(status = task.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                Chip(label = task.taskType.name)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Created ${task.createdAt.toRelativeTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (task.status != TaskStatus.COMPLETED) {
                    TextButton(onClick = onComplete) {
                        Text("Complete")
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
```

## Database Stack

### PostgreSQL 16+
**Purpose**: Primary relational database

**Why PostgreSQL**:
- **ACID Compliance**: Full transactional support with rollback
- **Performance**: Handles millions of records efficiently
- **JSON Support**: Native JSONB for flexible schemas
- **Full-Text Search**: Built-in text search with ranking
- **Extensions**: PostGIS, pg_trgm, uuid-ossp
- **Scalability**: Horizontal scaling with partitioning
- **Docker Ready**: Official images with excellent support

**Docker Deployment**:
```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD:-aitask_secret}
      POSTGRES_INITDB_ARGS: "-E UTF8 --locale=en_US.UTF-8"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aitask"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
    driver: local
```

**Connection Configuration**:
```kotlin
// application.yaml
database:
  host: ${DB_HOST:localhost}
  port: ${DB_PORT:5432}
  name: ${DB_NAME:aitask}
  user: ${DB_USER:aitask}
  password: ${DB_PASSWORD:aitask_secret}
  pool:
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000
    idleTimeout: 600000
    maxLifetime: 1800000
```

### Exposed ORM 0.50+
**Purpose**: Type-safe SQL framework for Kotlin

**Why Exposed**:
- **Type Safety**: Compile-time SQL validation
- **Kotlin DSL**: Fluent, readable queries
- **Transaction Support**: Built-in transaction management
- **DAO Pattern**: Optional DAO layer
- **Migration Support**: Works with Flyway
- **Multiple Databases**: PostgreSQL, MySQL, SQLite support

**Table Definitions**:
```kotlin
object Tasks : Table("tasks") {
    val id = uuid("id").autoGenerate()
    val title = varchar("title", 500)
    val description = text("description").nullable()
    val taskType = enumerationByName<TaskType>("task_type", 50)
    val status = enumerationByName<TaskStatus>("status", 50)
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val workspacePath = varchar("workspace_path", 1000).nullable()
    val branchName = varchar("branch_name", 200).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    val completedAt = timestamp("completed_at").nullable()
    
    override val primaryKey = PrimaryKey(id)
}

object Projects : Table("projects") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 200).uniqueIndex()
    val repositoryUrl = varchar("repository_url", 500)
    val description = text("description").nullable()
    val workspacePath = varchar("workspace_path", 1000)
    val branchTemplate = varchar("branch_template", 200).default("task-{taskId}")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(id)
}

// Repository Implementation
class TaskRepositoryImpl(private val database: Database) : TaskRepository {
    override suspend fun findById(id: UUID): Task? = withContext(Dispatchers.IO) {
        transaction(database) {
            Tasks.select { Tasks.id eq id }
        .singleOrNull()
        ?.toTask()
        }
    }
    
    override suspend fun findAll(): List<Task> = withContext(Dispatchers.IO) {
        transaction(database) {
            Tasks.selectAll()
                .orderBy(Tasks.createdAt to SortOrder.DESC)
                .map { it.toTask() }
        }
    }
    
    override suspend fun create(task: Task): Task = withContext(Dispatchers.IO) {
        transaction(database) {
            Tasks.insert {
                it[id] = task.id
                it[title] = task.title
                it[description] = task.description
                it[taskType] = task.taskType
                it[status] = task.status
                it[projectId] = task.projectId
                it[workspacePath] = task.workspacePath
                it[branchName] = task.branchName
            }
            task
        }
    }
    
    private fun ResultRow.toTask() = Task(
    id = this[Tasks.id],
    title = this[Tasks.title],
    description = this[Tasks.description],
    taskType = this[Tasks.taskType],
    status = this[Tasks.status],
        projectId = this[Tasks.projectId],
        workspacePath = this[Tasks.workspacePath],
        branchName = this[Tasks.branchName],
        createdAt = this[Tasks.createdAt].toInstant(),
        updatedAt = this[Tasks.updatedAt].toInstant(),
        completedAt = this[Tasks.completedAt]?.toInstant()
    )
}
```

### Flyway 10+
**Purpose**: Database migration management

**Migration Files**:
```sql
-- V1__initial_schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

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

CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_created ON tasks(created_at);
```

```sql
-- V2__add_rules_and_slack.sql
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    is_global BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, rule_id)
);

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

### HikariCP 5.1+
**Purpose**: High-performance JDBC connection pooling

```kotlin
object DatabaseFactory {
    fun createDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${config.host}:${config.port}/${config.name}"
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            
            // Pool Configuration
            maximumPoolSize = config.pool.maximumPoolSize
            minimumIdle = config.pool.minimumIdle
            connectionTimeout = config.pool.connectionTimeout
            idleTimeout = config.pool.idleTimeout
            maxLifetime = config.pool.maxLifetime
            
            // Performance
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            
            // Validation
            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
        }
        
        return HikariDataSource(hikariConfig)
    }
    
    fun initDatabase(dataSource: HikariDataSource): Database {
        // Run Flyway migrations
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
        
        flyway.migrate()
        
        // Create Exposed database connection
        return Database.connect(dataSource)
    }
}
```

## UI and Desktop Integration

### Material Design 3
**Purpose**: Modern UI component library

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260)
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        ),
        content = content
    )
}
```

### Kotlin Coroutines
**Purpose**: Structured concurrency

```kotlin
// ViewModel with StateFlow
class TaskViewModel(
    private val taskService: TaskService
) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadTasks()
    }
    
    fun loadTasks() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            
            try {
                val tasks = taskService.getAllTasks()
                _tasks.value = tasks
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun createTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            try {
                val task = taskService.createTask(request)
                _tasks.value = _tasks.value + task
            } catch (e: Exception) {
                _error.value = "Failed to create task: ${e.message}"
            }
        }
    }
}
```

## External Integrations

### JGit 6.9+
**Purpose**: Pure Java Git implementation

```kotlin
class GitService {
    suspend fun cloneRepository(
        url: String,
        directory: File,
        branch: String = "main"
    ): Git = withContext(Dispatchers.IO) {
Git.cloneRepository()
            .setURI(url)
            .setDirectory(directory)
            .setBranch(branch)
            .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
    .call()
    }
    
    suspend fun createBranch(
        workspace: File,
        branchName: String
    ): Ref = withContext(Dispatchers.IO) {
        Git.open(workspace).use { git ->
git.branchCreate()
    .setName(branchName)
    .call()
        }
    }
    
    suspend fun commitChanges(
        workspace: File,
        message: String
    ): RevCommit = withContext(Dispatchers.IO) {
        Git.open(workspace).use { git ->
            git.add().addFilepattern(".").call()
            git.commit().setMessage(message).call()
        }
    }
}
```

### Ktor Client 2.3+
**Purpose**: Async HTTP client for Slack API

```kotlin
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        })
    }
    
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 10000
    }
}

class SlackService(private val botToken: String) {
    suspend fun postMessage(channel: String, text: String): SlackResponse {
        return httpClient.post("https://slack.com/api/chat.postMessage") {
            header("Authorization", "Bearer $botToken")
            contentType(ContentType.Application.Json)
            setBody(SlackMessageRequest(channel, text))
        }.body()
    }
}
```

## Testing Stack

### JUnit 5 + Kotlin Test
```kotlin
class TaskServiceTest {
    private lateinit var taskService: TaskService
    private lateinit var taskRepository: TaskRepository
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        taskService = TaskService(taskRepository, mockk(), mockk())
    }
    
@Test
    fun `should create task successfully`() = runTest {
    // Given
    val request = CreateTaskRequest(
            title = "New Feature",
            taskType = TaskType.FEATURE,
        projectId = UUID.randomUUID()
    )
        
        coEvery { taskRepository.create(any()) } returns Task(/* ... */)
    
    // When
        val result = taskService.createTask(request)
    
    // Then
        assertEquals("New Feature", result.title)
        coVerify { taskRepository.create(any()) }
    }
}
```

## Build Commands Summary

```bash
# Compile
mvn clean compile

# Test
mvn test

# Package JAR
mvn package

# Run application
mvn exec:java

# Database migrations
mvn flyway:migrate

# Create native installer
mvn package jpackage:jpackage

# Docker build
docker build -t aitask:1.0.0 .
docker-compose up --build
```

## Recommended Versions

```
JDK: 21 (LTS)
Kotlin: 2.0.0
Maven: 3.9.6
PostgreSQL: 16.2
Compose Desktop: 1.6.0
Exposed: 0.50.0
JGit: 6.9.0
Ktor: 2.3.9
```

## Next Steps

1. Setup environment - [00-environment-setup.md](00-environment-setup.md)
2. Review architecture - [14-architecture-overview.md](14-architecture-overview.md)
3. Configure database - [03-database-design.md](03-database-design.md)
4. Start development - [01-run-and-debug.md](01-run-and-debug.md)
