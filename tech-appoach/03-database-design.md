# Database Design - PostgreSQL Schema

## Overview
PostgreSQL 16+ database schema for AiTask standalone application with support for tasks, projects, Git integration, Slack channels, and rule management. Designed for single-user or small team deployments.

## Database Architecture

### Design Principles
- **ACID Compliance**: Full transaction support with rollback capability
- **Referential Integrity**: Foreign key constraints ensure data consistency
- **Performance**: Comprehensive indexing strategy for fast queries
- **Scalability**: Schema supports thousands of tasks and projects
- **Flexibility**: JSONB columns for extensible metadata
- **Search**: Full-text search capabilities using PostgreSQL GIN indexes

### Database Connection

**Connection String**:
```
jdbc:postgresql://localhost:5432/aitask?ssl=false
Username: aitask
Password: aitask_secret
```

**Docker Deployment**:
```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
```

## Core Tables

### Projects Table

**Purpose**: Stores project information including repository URLs and workspace configurations.

**Schema**:
```sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL UNIQUE,
    repository_url VARCHAR(500) NOT NULL,
    description TEXT,
    workspace_path VARCHAR(1000) NOT NULL,
    branch_template VARCHAR(200) DEFAULT 'task-{taskId}',
    wiki_page VARCHAR(500),
    team VARCHAR(200),
    hub_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_repository_url CHECK (
        repository_url ~* '^(https?|git|ssh)://.*'
    ),
    CONSTRAINT valid_workspace_path CHECK (
        length(workspace_path) > 0
    )
);

-- Indexes for performance
CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX idx_projects_search ON projects 
    USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_projects_updated_at 
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

**Kotlin Entity**:
```kotlin
object Projects : Table("projects") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 200).uniqueIndex()
    val repositoryUrl = varchar("repository_url", 500)
    val description = text("description").nullable()
    val workspacePath = varchar("workspace_path", 1000)
    val branchTemplate = varchar("branch_template", 200).default("task-{taskId}")
    val wikiPage = varchar("wiki_page", 500).nullable()
    val team = varchar("team", 200).nullable()
    val hubUrl = varchar("hub_url", 500).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(id)
}

data class Project(
    val id: UUID,
    val name: String,
    val repositoryUrl: String,
    val description: String? = null,
    val workspacePath: String,
    val branchTemplate: String = "task-{taskId}",
    val wikiPage: String? = null,
    val team: String? = null,
    val hubUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Tasks Table

**Purpose**: Stores individual tasks with workspace, Git branch, and status information.

**Schema**:
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
        task_type IN ('FEATURE', 'BUG_FIX', 'REFACTOR', 'DOCUMENTATION', 'TESTING', 'RESEARCH')
    ),
    CONSTRAINT valid_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT completed_at_with_status CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL) OR
        (status != 'COMPLETED')
    )
);

-- Indexes
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at DESC);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at DESC) 
    WHERE completed_at IS NOT NULL;

-- Full-text search
CREATE INDEX idx_tasks_search ON tasks 
    USING gin(to_tsvector('english', 
        title || ' ' || COALESCE(description, '')
    ));

-- Update trigger
CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-set completed_at
CREATE OR REPLACE FUNCTION set_completed_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;
    IF NEW.status != 'COMPLETED' THEN
        NEW.completed_at = NULL;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_set_completed_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION set_completed_at();
```

**Kotlin Entity**:
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

enum class TaskType {
    FEATURE, BUG_FIX, REFACTOR, DOCUMENTATION, TESTING, RESEARCH
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}

data class Task(
    val id: UUID,
    val title: String,
    val description: String? = null,
    val taskType: TaskType,
    val status: TaskStatus,
    val projectId: UUID,
    val workspacePath: String? = null,
    val branchName: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null
)
```

### Rules Table

**Purpose**: Stores project-specific and global development rules/guidelines.

**Schema**:
```sql
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_name_length CHECK (length(name) > 0),
    CONSTRAINT valid_content_length CHECK (length(content) > 0)
);

CREATE INDEX idx_rules_category ON rules(category);
CREATE INDEX idx_rules_global ON rules(is_global);
CREATE INDEX idx_rules_name ON rules(name);
CREATE INDEX idx_rules_search ON rules 
    USING gin(to_tsvector('english', name || ' ' || COALESCE(category, '')));

CREATE TRIGGER update_rules_updated_at 
    BEFORE UPDATE ON rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

**Kotlin Entity**:
```kotlin
object Rules : Table("rules") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 200)
    val content = text("content")
    val category = varchar("category", 100).nullable()
    val isGlobal = bool("is_global").default(false)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(id)
}

data class Rule(
    val id: UUID,
    val name: String,
    val content: String,
    val category: String? = null,
    val isGlobal: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Project Rules (Junction Table)

**Purpose**: Many-to-many relationship between projects and rules.

**Schema**:
```sql
CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (project_id, rule_id)
);

CREATE INDEX idx_project_rules_project ON project_rules(project_id);
CREATE INDEX idx_project_rules_rule ON project_rules(rule_id);
```

**Kotlin Entity**:
```kotlin
object ProjectRules : Table("project_rules") {
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.CASCADE)
    val ruleId = uuid("rule_id").references(Rules.id, onDelete = ReferenceOption.CASCADE)
    val appliedAt = timestamp("applied_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(projectId, ruleId)
}
```

### Slack Channels Table

**Purpose**: Stores Slack channel information for project notifications.

**Schema**:
```sql
CREATE TABLE slack_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slack_channel_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_slack_channel_id CHECK (
        slack_channel_id ~* '^C[A-Z0-9]{10}$'
    )
);

CREATE INDEX idx_slack_channels_slack_id ON slack_channels(slack_channel_id);
CREATE INDEX idx_slack_channels_project ON slack_channels(project_id);
CREATE INDEX idx_slack_channels_name ON slack_channels(name);

CREATE TRIGGER update_slack_channels_updated_at 
    BEFORE UPDATE ON slack_channels
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

**Kotlin Entity**:
```kotlin
object SlackChannels : Table("slack_channels") {
    val id = uuid("id").autoGenerate()
    val slackChannelId = varchar("slack_channel_id", 100).uniqueIndex()
    val name = varchar("name", 200)
    val description = text("description").nullable()
    val isPrivate = bool("is_private").default(false)
    val projectId = uuid("project_id").references(Projects.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
    
    override val primaryKey = PrimaryKey(id)
}

data class SlackChannel(
    val id: UUID,
    val slackChannelId: String,
    val name: String,
    val description: String? = null,
    val isPrivate: Boolean = false,
    val projectId: UUID? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## Flyway Migrations

### V1__initial_schema.sql

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Projects table
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL UNIQUE,
    repository_url VARCHAR(500) NOT NULL,
    description TEXT,
    workspace_path VARCHAR(1000) NOT NULL,
    branch_template VARCHAR(200) DEFAULT 'task-{taskId}',
    wiki_page VARCHAR(500),
    team VARCHAR(200),
    hub_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_repository_url CHECK (
        repository_url ~* '^(https?|git|ssh)://.*'
    )
);

-- Tasks table
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
        task_type IN ('FEATURE', 'BUG_FIX', 'REFACTOR', 'DOCUMENTATION', 'TESTING', 'RESEARCH')
    ),
    CONSTRAINT valid_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    )
);

-- Rules table
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    is_global BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Project-Rules junction table
CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, rule_id)
);

-- Slack channels table
CREATE TABLE slack_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    slack_channel_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Update timestamp function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Auto-set completed_at function
CREATE OR REPLACE FUNCTION set_completed_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;
    IF NEW.status != 'COMPLETED' THEN
        NEW.completed_at = NULL;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_projects_updated_at 
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rules_updated_at 
    BEFORE UPDATE ON rules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_slack_channels_updated_at 
    BEFORE UPDATE ON slack_channels
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Trigger for task completion
CREATE TRIGGER trigger_set_completed_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION set_completed_at();
```

### V2__add_indexes.sql

```sql
-- Projects indexes
CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);

-- Tasks indexes
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_updated_at ON tasks(updated_at DESC);
CREATE INDEX idx_tasks_completed_at ON tasks(completed_at DESC) 
    WHERE completed_at IS NOT NULL;

-- Rules indexes
CREATE INDEX idx_rules_category ON rules(category);
CREATE INDEX idx_rules_global ON rules(is_global);
CREATE INDEX idx_rules_name ON rules(name);

-- Project rules indexes
CREATE INDEX idx_project_rules_project ON project_rules(project_id);
CREATE INDEX idx_project_rules_rule ON project_rules(rule_id);

-- Slack channels indexes
CREATE INDEX idx_slack_channels_slack_id ON slack_channels(slack_channel_id);
CREATE INDEX idx_slack_channels_project ON slack_channels(project_id);
CREATE INDEX idx_slack_channels_name ON slack_channels(name);
```

### V3__add_full_text_search.sql

```sql
-- Full-text search indexes for projects
CREATE INDEX idx_projects_search ON projects 
    USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- Full-text search indexes for tasks
CREATE INDEX idx_tasks_search ON tasks 
    USING gin(to_tsvector('english', 
        title || ' ' || COALESCE(description, '')
    ));

-- Full-text search indexes for rules
CREATE INDEX idx_rules_search ON rules 
    USING gin(to_tsvector('english', name || ' ' || COALESCE(category, '')));
```

## Database Operations

### Common Queries

**Find tasks by project**:
```sql
SELECT t.*, p.name as project_name
FROM tasks t
JOIN projects p ON t.project_id = p.id
WHERE p.id = ?
ORDER BY t.created_at DESC;
```

**Search tasks by text**:
```sql
SELECT *
FROM tasks
WHERE to_tsvector('english', title || ' ' || COALESCE(description, ''))
    @@ plainto_tsquery('english', 'feature authentication')
ORDER BY ts_rank(
    to_tsvector('english', title || ' ' || COALESCE(description, '')),
    plainto_tsquery('english', 'feature authentication')
) DESC;
```

**Get project with rules**:
```sql
SELECT 
    p.*,
    array_agg(r.id) as rule_ids,
    array_agg(r.name) as rule_names
FROM projects p
LEFT JOIN project_rules pr ON p.id = pr.project_id
LEFT JOIN rules r ON pr.rule_id = r.id
WHERE p.id = ?
GROUP BY p.id;
```

**Task statistics by project**:
```sql
SELECT 
    p.name as project_name,
    COUNT(t.id) as total_tasks,
    COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) as completed,
    COUNT(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 END) as in_progress,
    COUNT(CASE WHEN t.status = 'PENDING' THEN 1 END) as pending
FROM projects p
LEFT JOIN tasks t ON p.id = t.project_id
GROUP BY p.id, p.name
ORDER BY total_tasks DESC;
```

## Database Maintenance

### Backup and Restore

**Backup**:
```bash
# Full backup
docker exec aitask-postgres pg_dump -U aitask aitask > backup_$(date +%Y%m%d).sql

# Backup with compression
docker exec aitask-postgres pg_dump -U aitask aitask | gzip > backup_$(date +%Y%m%d).sql.gz

# Backup to Docker volume
docker exec aitask-postgres pg_dump -U aitask aitask -f /var/lib/postgresql/backups/backup.sql
```

**Restore**:
```bash
# Restore from file
cat backup_20240101.sql | docker exec -i aitask-postgres psql -U aitask aitask

# Restore compressed
gunzip -c backup_20240101.sql.gz | docker exec -i aitask-postgres psql -U aitask aitask
```

### Database Statistics

**Table sizes**:
```sql
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY size_bytes DESC;
```

**Index usage**:
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### Performance Tuning

**Analyze tables**:
```sql
ANALYZE tasks;
ANALYZE projects;
ANALYZE rules;
```

**Vacuum**:
```sql
VACUUM ANALYZE tasks;
VACUUM ANALYZE projects;
```

**Reindex**:
```sql
REINDEX TABLE tasks;
REINDEX INDEX idx_tasks_search;
```

## Connection Pooling (HikariCP)

**Configuration**:
```kotlin
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/aitask"
    username = "aitask"
    password = "aitask_secret"
    driverClassName = "org.postgresql.Driver"
    
    // Pool sizing
    maximumPoolSize = 10
    minimumIdle = 5
    
    // Timeouts (milliseconds)
    connectionTimeout = 30000
    idleTimeout = 600000
    maxLifetime = 1800000
    
    // Performance
    addDataSourceProperty("cachePrepStmts", "true")
    addDataSourceProperty("prepStmtCacheSize", "250")
    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    
    // Validation
    connectionTestQuery = "SELECT 1"
    validationTimeout = 5000
}

val dataSource = HikariDataSource(config)
```

## Related Documentation

- [Architecture Overview](14-architecture-overview.md)
- [Technology Stack](01-technology-stack.md)
- [Task Management](02-task-management.md)
- [Docker Setup](05-docker-setup.md)
