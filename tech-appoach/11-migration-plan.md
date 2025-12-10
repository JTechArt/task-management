# Migration Plan: TypeScript/SQLite to Kotlin/PostgreSQL

## Overview
Comprehensive migration strategy for transforming the Electron desktop application to a Kotlin/Spring Boot web application with PostgreSQL.

## Migration Phases

### Phase 1: Foundation (2-3 weeks)
**Goal**: Set up Kotlin/Spring Boot project and PostgreSQL database

**Tasks**:
1. Create Spring Boot project structure
2. Set up PostgreSQL with Docker
3. Create database schema with Flyway
4. Implement basic CRUD for one entity (Task or Project)
5. Set up CI/CD pipeline

**Deliverables**:
- Working Spring Boot application
- PostgreSQL database with migrations
- Basic REST API endpoint
- Docker configuration
- Tests for basic functionality

### Phase 2: Core Services (3-4 weeks)
**Goal**: Migrate core business logic to Kotlin

**Tasks**:
1. Migrate TaskService to Kotlin
   - Convert TypeScript service to Kotlin
   - Implement repository layer
   - Add validation and error handling
   - Write unit tests

2. Migrate ProjectService to Kotlin
   - Convert project management logic
   - Handle project-task relationships
   - Implement REST endpoints

3. Migrate RuleService to Kotlin
   - Convert rule management
   - Implement rule-project associations

4. Implement Authentication
   - JWT-based authentication
   - User registration/login
   - Password hashing with BCrypt

**Deliverables**:
- Task Management API complete
- Project Management API complete
- Rule Management API complete
- Authentication system working
- Comprehensive test coverage (>80%)

### Phase 3: Integrations (2-3 weeks)
**Goal**: Migrate external integrations

**Tasks**:
1. Git Integration
   - Server-side Git operations
   - Support GitHub/GitLab/Bitbucket
   - Handle authentication

2. Slack Integration
   - Bot API integration
   - Channel management
   - Message posting

3. Visual Mockup Generation
   - Server-side mockup generation
   - File storage strategy
   - Template management

**Deliverables**:
- Git integration API
- Slack integration API
- Mockup generation service
- Integration tests

### Phase 4: Frontend Adaptation (2-3 weeks)
**Goal**: Adapt React frontend for web deployment

**Tasks**:
1. Remove Electron-specific code
   - Replace IPC calls with HTTP requests
   - Remove Node.js dependencies
   - Update state management

2. Add authentication UI
   - Login/register pages
   - Token management
   - Protected routes

3. Responsive design
   - Mobile-friendly layouts
   - Progressive Web App (PWA) features
   - Offline mode with service workers

4. Testing
   - Component tests
   - E2E tests with Playwright
   - Cross-browser testing

**Deliverables**:
- Web-based React application
- Authentication UI
- Responsive design
- PWA features
- E2E tests passing

### Phase 5: Data Migration (1-2 weeks)
**Goal**: Migrate existing data from SQLite to PostgreSQL

**Tasks**:
1. Export data from SQLite
2. Transform data format
3. Import to PostgreSQL
4. Verify data integrity
5. Provide migration tools for users

**Deliverables**:
- Migration scripts
- Data validation tools
- User migration guide

### Phase 6: Deployment (1-2 weeks)
**Goal**: Deploy to production

**Tasks**:
1. Production Docker configuration
2. Set up hosting (AWS/GCP/Azure)
3. Configure CI/CD
4. Set up monitoring and logging
5. Database backup strategy
6. SSL/TLS certificates

**Deliverables**:
- Production environment
- Automated deployments
- Monitoring dashboards
- Backup procedures
- Documentation

## Data Migration Strategy

### Export from SQLite

```kotlin
class SQLiteExporter(private val dbPath: String) {
    fun exportProjects(): List<ProjectExport> {
        val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery("SELECT * FROM projects")
        
        val projects = mutableListOf<ProjectExport>()
        while (resultSet.next()) {
            projects.add(ProjectExport(
                id = resultSet.getString("id"),
                name = resultSet.getString("name"),
                repositoryUrl = resultSet.getString("repositoryUrl"),
                createdAt = resultSet.getLong("createdAt")
            ))
        }
        
        return projects
    }
}
```

### Transform Data

```kotlin
class DataTransformer {
    fun transformProject(export: ProjectExport): Project {
        return Project(
            id = UUID.fromString(export.id),
            name = export.name,
            repositoryUrl = export.repositoryUrl,
            createdAt = Instant.ofEpochMilli(export.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        )
    }
}
```

### Import to PostgreSQL

```kotlin
class PostgresImporter(private val projectRepository: ProjectRepository) {
    fun importProjects(projects: List<Project>) {
        projectRepository.saveAll(projects)
    }
}
```

## API Migration Guide

### From IPC to REST

**Before (Electron IPC)**:
```typescript
// Renderer process
const tasks = await window.electron.invoke('get-all-tasks');
```

**After (REST API)**:
```typescript
// Web frontend
const response = await axios.get('/api/tasks', {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
const tasks = response.data;
```

### Authentication Addition

**Before**: No authentication (single-user desktop app)

**After**: JWT-based authentication

```typescript
// Login
const loginResponse = await axios.post('/api/auth/login', {
  username: 'user',
  password: 'password'
});
const token = loginResponse.data.token;
localStorage.setItem('token', token);

// Authenticated request
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
```

## Code Migration Examples

### Service Migration

**TypeScript (Before)**:
```typescript
class TaskService {
  private db: Database;
  
  async createTask(taskData: TaskInput): Promise<Task> {
    const id = uuidv4();
    const task = {
      id,
      ...taskData,
      createdAt: Date.now()
    };
    
    return new Promise((resolve, reject) => {
      this.db.run(
        'INSERT INTO tasks (id, title, ...) VALUES (?, ?, ...)',
        [task.id, task.title, ...],
        (err) => {
          if (err) reject(err);
          else resolve(task);
        }
      );
    });
  }
}
```

**Kotlin (After)**:
```kotlin
@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun createTask(request: CreateTaskRequest, userId: UUID): TaskDto {
        val user = userRepository.findById(userId)
            .orElseThrow { NotFoundException("User not found") }
        
        val task = Task(
            title = request.title,
            description = request.description,
            taskType = request.taskType,
            createdBy = user
        )
        
        val savedTask = taskRepository.save(task)
        return savedTask.toDto()
    }
}
```

## Compatibility Layer

For gradual migration, create a compatibility layer:

```kotlin
@RestController
@RequestMapping("/api/compat")
class CompatibilityController(private val taskService: TaskService) {
    
    // Mimics Electron IPC interface
    @PostMapping("/invoke")
    fun invoke(@RequestBody request: IPCRequest): ResponseEntity<Any> {
        return when (request.channel) {
            "get-all-tasks" -> ResponseEntity.ok(taskService.findAll())
            "create-task" -> {
                val taskRequest = objectMapper.readValue(
                    request.args[0],
                    CreateTaskRequest::class.java
                )
                ResponseEntity.ok(taskService.create(taskRequest))
            }
            else -> ResponseEntity.notFound().build()
        }
    }
}

data class IPCRequest(
    val channel: String,
    val args: List<String>
)
```

## Testing Strategy During Migration

### Parallel Testing
Run both systems simultaneously and compare results:

```kotlin
@Test
fun `migrated service produces same results as original`() {
    // Call original TypeScript API
    val originalResult = callOriginalAPI("/api/tasks")
    
    // Call new Kotlin API
    val newResult = restTemplate.getForObject("/api/tasks", Array<TaskDto>::class.java)
    
    // Compare results
    assertThat(newResult).isEqualTo(originalResult)
}
```

### Feature Flags
Use feature flags to gradually roll out new backend:

```kotlin
@Service
class FeatureFlagService {
    fun useKotlinBackend(userId: UUID): Boolean {
        // Gradually enable for users
        return userId.hashCode() % 100 < rolloutPercentage
    }
}
```

## Rollback Strategy

1. **Database**: Keep SQLite data during transition
2. **API**: Maintain both old and new endpoints
3. **Frontend**: Feature toggle to switch backends
4. **Data**: Regular backups before migration
5. **Monitoring**: Track error rates during rollout

## Timeline Summary

| Phase | Duration | Team Size | Dependencies |
|-------|----------|-----------|--------------|
| 1. Foundation | 2-3 weeks | 2 developers | None |
| 2. Core Services | 3-4 weeks | 2-3 developers | Phase 1 |
| 3. Integrations | 2-3 weeks | 2 developers | Phase 2 |
| 4. Frontend | 2-3 weeks | 1-2 developers | Phase 2 |
| 5. Data Migration | 1-2 weeks | 1 developer | Phases 2,4 |
| 6. Deployment | 1-2 weeks | 1 developer + DevOps | All phases |
| **Total** | **11-17 weeks** | **2-3 developers** | - |

## Risk Mitigation

### Technical Risks
- **Database migration issues**: Test with production-like data
- **Performance degradation**: Load testing before deployment
- **Integration failures**: Comprehensive integration tests

### Business Risks
- **User disruption**: Phased rollout with rollback capability
- **Data loss**: Multiple backups and verification
- **Feature gaps**: Feature parity checklist

## Success Criteria

- All features from desktop version working
- Response time < 500ms for 95% of requests
- 99.9% uptime
- Zero data loss during migration
- User acceptance > 90%
- Test coverage > 80%

See [10-deployment-guide.md](10-deployment-guide.md) for production deployment details.
