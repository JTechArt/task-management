# Run and Debug Guide - Maven/Kotlin Application

## Overview
This document provides comprehensive instructions for running, debugging, and testing the AiTask standalone application built with Maven, Kotlin, Compose Desktop, and PostgreSQL.

## Quick Start Commands

```bash
# Start PostgreSQL database
docker-compose up -d postgres

# Run application (development)
mvn clean compile compose:run

# Run tests
mvn test

# Package application
mvn clean package

# Run packaged JAR
java -jar target/aitask-kotlin-1.0.0.jar

# Full Docker stack
docker-compose up --build
```

## Prerequisites

Before running the application:
- [ ] JDK 21 installed
- [ ] Maven 3.9+ installed
- [ ] Docker Desktop running
- [ ] PostgreSQL container started
- [ ] Environment variables configured

## Running the Application

### 1. Development Mode (Local)

**Description**: Runs the application locally with hot reload support and database in Docker.

**Step-by-step**:
```bash
# 1. Start PostgreSQL
docker-compose up -d postgres

# 2. Wait for PostgreSQL to be ready
docker-compose logs -f postgres
# Wait for: "database system is ready to accept connections"

# 3. Run database migrations
mvn flyway:migrate

# 4. Compile and run application
mvn clean compile compose:run
```

**Alternative with Maven Exec**:
```bash
mvn compile exec:java \
  -Dexec.mainClass="com.aitask.AitaskApplicationKt" \
  -Dexec.args="--dev"
```

**Features**:
- Fast compilation with incremental builds
- Hot reload for code changes (restart required)
- Full logging output
- Direct access to source code for debugging
- PostgreSQL in Docker container

**When to Use**:
- Active development
- Testing features
- Database schema development
- API development

**Environment Variables**:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=aitask
export DB_USER=aitask
export DB_PASSWORD=aitask_secret
export LOG_LEVEL=DEBUG
```

### 2. Production Mode (JAR)

**Description**: Runs the application from compiled JAR with optimized settings.

**Build and Run**:
```bash
# 1. Build optimized JAR
mvn clean package -DskipTests

# 2. Run JAR
java -jar target/aitask-kotlin-1.0.0.jar

# Or with JVM options
java -Xms256m -Xmx1024m -jar target/aitask-kotlin-1.0.0.jar
```

**Features**:
- Optimized bytecode
- Production logging levels
- Smaller memory footprint
- Fast startup
- No development dependencies

**When to Use**:
- Production deployment
- Performance testing
- User testing
- Release candidates

### 3. Docker Mode (Full Stack)

**Description**: Runs entire application stack (app + database) in Docker containers.

**Run Full Stack**:
```bash
# Build and start all services
docker-compose up --build

# Or in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f app
docker-compose logs -f postgres

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

**Features**:
- Consistent environment
- Isolated from host system
- Easy deployment
- Automatic service orchestration
- Volume persistence

**When to Use**:
- Production deployment
- CI/CD pipelines
- Multi-container testing
- Deployment simulation

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD:-aitask_secret}
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
      DB_PASSWORD: ${DB_PASSWORD:-aitask_secret}
    volumes:
      - ./workspaces:/app/workspaces
      - ./config:/app/config
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

### 4. Native Installer Mode

**Description**: Creates platform-specific native installers using JPackage.

**Create Installer**:
```bash
# Package application
mvn clean package

# Create native installer for current OS
mvn jpackage:jpackage

# Output location:
# - macOS: target/dist/AiTask-1.0.0.dmg
# - Windows: target/dist/AiTask-1.0.0.msi
# - Linux: target/dist/aitask-1.0.0.deb
```

**Install and Run**:
```bash
# macOS
open target/dist/AiTask-1.0.0.dmg
# Drag to Applications, then open

# Windows
target\dist\AiTask-1.0.0.msi
# Follow installer wizard

# Linux (Ubuntu/Debian)
sudo dpkg -i target/dist/aitask-1.0.0.deb
aitask  # Run from terminal
```

**When to Use**:
- End-user distribution
- Desktop application deployment
- Beta testing with users
- Release builds

## Debugging

### 1. IntelliJ IDEA Debugging

**Setup Debug Configuration**:
1. Open project in IntelliJ IDEA
2. Go to `Run` → `Edit Configurations`
3. Click `+` → `Kotlin`
4. Configure:
   - **Name**: "AiTask Debug"
   - **Main class**: `com.aitask.AitaskApplicationKt`
   - **VM options**: `-Xms256m -Xmx1024m`
   - **Environment variables**: `DB_HOST=localhost;DB_PORT=5432;DB_NAME=aitask;DB_USER=aitask;DB_PASSWORD=aitask_secret`
   - **Working directory**: `$PROJECT_DIR$`
   - **Use classpath of module**: `aitask-kotlin`

**Start Debugging**:
```bash
# 1. Start PostgreSQL
docker-compose up -d postgres

# 2. Set breakpoints in code
# Click left gutter in editor

# 3. Click Debug icon (or Shift+F9)
```

**Debugging Features**:
- **Breakpoints**: Click line numbers to add/remove
- **Step Over** (F8): Execute current line
- **Step Into** (F7): Enter function calls
- **Step Out** (Shift+F8): Exit current function
- **Resume** (F9): Continue to next breakpoint
- **Evaluate Expression** (Alt+F8): Test code at runtime
- **Watches**: Monitor variable values
- **Threads**: View coroutine execution

**Debug Compose UI**:
```kotlin
// Enable Compose debug mode
@Composable
fun MyScreen() {
    // Add debug logging
    SideEffect {
        println("MyScreen recomposed")
    }
    
    // Use remember with calculation lambda
    val data = remember(key1) {
        println("Recalculating data for key: $key1")
        calculateData(key1)
    }
}
```

### 2. Remote Debugging (Docker)

**Dockerfile with Debug Support**:
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/aitask-kotlin-*.jar app.jar

# Expose debug port
EXPOSE 8080 5005

# Enable remote debugging
ENTRYPOINT ["java", \
  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
  "-jar", "app.jar"]
```

**docker-compose.yml with Debug**:
```yaml
app:
  build: .
  ports:
    - "8080:8080"
    - "5005:5005"  # Debug port
  environment:
    - JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

**Connect from IntelliJ IDEA**:
1. `Run` → `Edit Configurations` → `+` → `Remote JVM Debug`
2. Configure:
   - **Name**: "Docker Debug"
   - **Host**: `localhost`
   - **Port**: `5005`
   - **Debugger mode**: `Attach to remote JVM`
3. Start Docker container: `docker-compose up`
4. Click Debug (this configuration) to attach

### 3. Log-Based Debugging

**Configure Logging (logback.xml)**:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/aitask.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/aitask.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Application logging -->
    <logger name="com.aitask" level="DEBUG"/>
    
    <!-- Database logging -->
    <logger name="org.jetbrains.exposed" level="DEBUG"/>
    <logger name="org.postgresql" level="INFO"/>
    
    <!-- HTTP client logging -->
    <logger name="io.ktor.client" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Add Debug Logging**:
```kotlin
import org.slf4j.LoggerFactory

class TaskService {
    private val logger = LoggerFactory.getLogger(TaskService::class.java)
    
    suspend fun createTask(request: CreateTaskRequest): Task {
        logger.debug("Creating task: ${request.title}")
        
        try {
            val task = taskRepository.create(request.toTask())
            logger.info("Task created successfully: ${task.id}")
            return task
        } catch (e: Exception) {
            logger.error("Failed to create task", e)
            throw e
        }
    }
}
```

**View Logs**:
```bash
# Real-time log viewing
tail -f logs/aitask.log

# Search logs
grep "ERROR" logs/aitask.log

# View last 100 lines
tail -100 logs/aitask.log

# Docker logs
docker-compose logs -f app
```

### 4. Database Debugging

**Connect to PostgreSQL**:
```bash
# Connect via Docker
docker exec -it aitask-postgres psql -U aitask -d aitask

# Inside psql:
# View current connections
SELECT * FROM pg_stat_activity WHERE datname = 'aitask';

# View slow queries
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

# Explain query plan
EXPLAIN ANALYZE SELECT * FROM tasks WHERE project_id = '...';

# View table sizes
SELECT 
    schemaname as schema,
    tablename as table,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Enable SQL Logging**:
```yaml
# application.yaml
database:
  logging:
    sql: true
    sqlParameters: true
```

```kotlin
// In code
Database.connect(dataSource).apply {
    // Enable statement logging
    TransactionManager.manager.defaultRepetitionAttempts = 3
    logger.addLogger(Slf4jSqlDebugLogger)
}
```

**Monitor Queries**:
```kotlin
// Log slow queries
object SlowQueryLogger : SqlLogger {
    private val threshold = 1000L // 1 second
    
    override fun log(context: StatementContext, transaction: Transaction) {
        val duration = context.duration
        if (duration > threshold) {
            logger.warn("Slow query (${duration}ms): ${context.expandArgs(transaction)}")
        }
    }
}
```

## Testing

### 1. Unit Tests

**Run All Tests**:
```bash
mvn test
```

**Run Specific Test Class**:
```bash
mvn test -Dtest=TaskServiceTest
```

**Run Specific Test Method**:
```bash
mvn test -Dtest=TaskServiceTest#shouldCreateTask
```

**Run with Coverage**:
```bash
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

**Example Unit Test**:
```kotlin
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals

class TaskServiceTest {
    private lateinit var taskService: TaskService
    private lateinit var taskRepository: TaskRepository
    private lateinit var projectRepository: ProjectRepository
    private lateinit var gitService: GitService
    
    @BeforeEach
    fun setup() {
        taskRepository = mockk()
        projectRepository = mockk()
        gitService = mockk()
        taskService = TaskService(taskRepository, projectRepository, gitService)
    }
    
    @Test
    fun `should create task successfully`() = runTest {
        // Given
        val projectId = UUID.randomUUID()
        val project = Project(
            id = projectId,
            name = "Test Project",
            repositoryUrl = "https://github.com/test/repo.git",
            workspacePath = "/tmp/workspace"
        )
        
        val request = CreateTaskRequest(
            title = "New Feature",
            description = "Implement feature X",
            taskType = TaskType.FEATURE,
            projectId = projectId
        )
        
        coEvery { projectRepository.findById(projectId) } returns project
        coEvery { gitService.isRepositoryCloned(any()) } returns true
        coEvery { gitService.createBranch(any(), any()) } just Runs
        coEvery { taskRepository.create(any()) } answers { firstArg() }
        
        // When
        val result = taskService.createTask(request)
        
        // Then
        assertEquals("New Feature", result.title)
        assertEquals(TaskType.FEATURE, result.taskType)
        coVerify { gitService.createBranch(any(), any()) }
        coVerify { taskRepository.create(any()) }
    }
}
```

### 2. Integration Tests

**Run Integration Tests**:
```bash
# Requires PostgreSQL running
docker-compose up -d postgres

mvn verify -P integration-tests
```

**Example Integration Test**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskRepositoryIntegrationTest {
    private lateinit var database: Database
    private lateinit var repository: TaskRepository
    
    @BeforeAll
    fun setup() {
        // Setup test database
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/aitask_test"
            username = "aitask"
            password = "aitask_secret"
        }
        val dataSource = HikariDataSource(config)
        database = Database.connect(dataSource)
        
        // Run migrations
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
        
        repository = TaskRepositoryImpl(database)
    }
    
    @Test
    fun `should persist and retrieve task`() = runTest {
        // Given
        val task = Task(
            id = UUID.randomUUID(),
            title = "Test Task",
            taskType = TaskType.FEATURE,
            status = TaskStatus.PENDING,
            projectId = UUID.randomUUID(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // When
        repository.create(task)
        val retrieved = repository.findById(task.id)
        
        // Then
        assertNotNull(retrieved)
        assertEquals(task.title, retrieved.title)
        assertEquals(task.taskType, retrieved.taskType)
    }
    
    @AfterAll
    fun tearDown() {
        // Clean up test data
        transaction(database) {
            Tasks.deleteAll()
            Projects.deleteAll()
        }
    }
}
```

### 3. UI Tests (Compose)

**Example UI Test**:
```kotlin
@OptIn(ExperimentalTestApi::class)
class TaskListScreenTest {
    @Test
    fun `should display task list`() = runComposeUiTest {
        // Given
        val tasks = listOf(
            Task(
                id = UUID.randomUUID(),
                title = "Task 1",
                taskType = TaskType.FEATURE,
                status = TaskStatus.PENDING,
                projectId = UUID.randomUUID(),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val viewModel = mockk<TaskViewModel> {
            every { tasks } returns MutableStateFlow(tasks)
        }
        
        // When
        setContent {
            TaskListScreen(viewModel = viewModel)
        }
        
        // Then
        onNodeWithText("Task 1").assertExists()
        onNodeWithText("FEATURE").assertExists()
    }
}
```

## Performance Profiling

### 1. JVM Profiling

**Enable JFR (Java Flight Recorder)**:
```bash
java \
  -XX:+FlightRecorder \
  -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
  -jar target/aitask-kotlin-1.0.0.jar

# Analyze with JDK Mission Control
jmc recording.jfr
```

**VisualVM Profiling**:
```bash
# Install VisualVM
brew install --cask visualvm

# Run application
java -jar target/aitask-kotlin-1.0.0.jar

# Open VisualVM and attach to process
visualvm
```

### 2. Database Profiling

**Enable pg_stat_statements**:
```sql
-- In PostgreSQL
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- View query statistics
SELECT 
    calls,
    total_time,
    mean_time,
    query
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

### 3. Application Metrics

**Add Metrics**:
```kotlin
class TaskService {
    private var createTaskDuration = 0L
    private var createTaskCount = 0L
    
    suspend fun createTask(request: CreateTaskRequest): Task {
        val start = System.currentTimeMillis()
        try {
            return doCreateTask(request)
        } finally {
            val duration = System.currentTimeMillis() - start
            createTaskDuration += duration
            createTaskCount++
            
            if (duration > 1000) {
                logger.warn("Slow task creation: ${duration}ms")
            }
        }
    }
    
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "averageCreateTime" to (createTaskDuration / createTaskCount),
            "totalCreated" to createTaskCount
        )
    }
}
```

## Troubleshooting

### Application Won't Start

```bash
# Check Java version
java -version  # Should be 21.x

# Check Maven version
mvn -version

# Check PostgreSQL
docker-compose ps postgres

# View application logs
cat logs/aitask.log | tail -100

# Check port conflicts
lsof -i :8080
```

### Database Connection Fails

```bash
# Test PostgreSQL connection
docker exec -it aitask-postgres psql -U aitask -d aitask

# Check connection string
echo $DB_HOST $DB_PORT $DB_NAME $DB_USER

# View PostgreSQL logs
docker-compose logs postgres | tail -50

# Reset database
docker-compose down -v
docker-compose up -d postgres
mvn flyway:migrate
```

### Compose UI Not Rendering

```bash
# Check display (Linux)
echo $DISPLAY

# Install required libraries (Linux)
sudo apt install libx11-6 libxrender1 libxtst6

# Check graphics driver
java -Dsun.java2d.trace=log -jar target/aitask-*.jar
```

## Maven Lifecycle Commands

```bash
# Clean build artifacts
mvn clean

# Compile source code
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Install to local repository
mvn install

# Deploy to remote repository
mvn deploy

# Skip tests
mvn package -DskipTests

# Offline mode (use cached dependencies)
mvn -o package

# Update dependencies
mvn clean install -U

# Generate project site
mvn site

# Display dependency tree
mvn dependency:tree

# Check for plugin updates
mvn versions:display-plugin-updates
```

## Next Steps

- **Task Management Implementation** - [02-task-management.md](02-task-management.md)
- **Database Schema Details** - [03-database-design.md](03-database-design.md)
- **Docker Deployment** - [05-docker-setup.md](05-docker-setup.md)
- **Git Integration** - [04-git-integration.md](04-git-integration.md)
