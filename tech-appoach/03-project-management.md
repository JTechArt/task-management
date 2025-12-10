# Project Management Feature

## Overview
Project Management provides centralized repository and project configuration management, enabling users to organize development work across multiple repositories and maintain project-specific settings.

## What This Feature Does
The Project Management feature enables users to:
- Create and manage software projects with repository associations
- Store project metadata (name, URL, description, team info)
- Associate custom rules with projects
- Import/export project configurations
- Clone projects with all settings
- Manage project lifecycle
- Configure project-specific workspace settings

## Acceptance Criteria

### Functional Requirements
✅ **Project CRUD Operations**
- Create projects with name, repository URL, and metadata
- Read project details with associated tasks
- Update project information
- Delete projects with cascade task handling

✅ **Repository Management**
- Support multiple Git providers (GitHub, GitLab, Bitbucket)
- Validate repository URLs
- Clone repositories to local workspace
- Track repository credentials

✅ **Project Configuration**
- Custom workspace paths per project
- Branch naming templates
- IDE preferences
- Build and run scripts

✅ **Rule Association**
- Attach custom Cursor AI rules to projects
- Manage project-specific rule sets
- Share rules across projects
- Import/export rules with projects

✅ **Project Import/Export**
- Export project configuration as JSON
- Import projects from JSON files
- Migrate projects between installations
- Backup and restore project settings

### Non-Functional Requirements
✅ **Performance**
- Project list load < 300ms
- Project creation < 1 second
- Support 100+ projects efficiently

✅ **Reliability**
- Database transaction safety
- Repository URL validation
- Cascade delete handling

## Technology Stack

### Backend Services

**Primary Service**: `ProjectService.ts`
```typescript
Location: src/main/services/ProjectService.ts
Language: TypeScript 5.2+
Database: SQLite3
```

**Related Services**:
- `EnhancedProjectService.ts`: Advanced project operations
- `ProjectExportService.ts`: Project export functionality
- `ProjectImportService.ts`: Project import functionality
- `ProjectMigrationService.ts`: Project data migration
- `GitProviderRegistry.ts`: Multi-provider Git support

### Database Schema

```sql
CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    repositoryUrl TEXT NOT NULL,
    description TEXT,
    wikiPage TEXT,
    team TEXT,
    hubUrl TEXT,
    workspacePath TEXT,
    branchTemplate TEXT DEFAULT 'task-{taskId}',
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

CREATE TABLE project_rules (
    projectId TEXT NOT NULL,
    ruleId TEXT NOT NULL,
    PRIMARY KEY (projectId, ruleId),
    FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (ruleId) REFERENCES rules(id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_project_rules_project ON project_rules(projectId);
```

### Frontend Components

**Primary Components**:
- `ProjectList.tsx`: Display and manage projects
- `CreateProject.tsx`: Project creation dialog
- `ProjectManagement.tsx`: Project settings and configuration

## Implementation Architecture

### Project Creation Flow

```
User Input
    ↓
CreateProject.tsx (validate)
    ↓
IPC: create-project
    ↓
ProjectService.createProject()
    ↓
┌──────────────────────────────────┐
│ 1. Validate repository URL       │
│ 2. Check duplicate project name  │
│ 3. Generate unique project ID    │
│ 4. Create database entry         │
│ 5. Initialize workspace folder   │
│ 6. Clone repository (optional)   │
│ 7. Apply default rules           │
│ 8. Return project details        │
└──────────────────────────────────┘
    ↓
Update UI with new project
```

### Project Export Flow

```
User Action: Export Project
    ↓
IPC: export-project
    ↓
ProjectExportService.exportProject()
    ↓
┌──────────────────────────────────┐
│ 1. Retrieve project data         │
│ 2. Fetch associated rules        │
│ 3. Collect task references       │
│ 4. Package configurations        │
│ 5. Generate JSON export          │
│ 6. Save to file                  │
└──────────────────────────────────┘
    ↓
Download exported JSON file
```

## Key Classes and Methods

### ProjectService

```typescript
class ProjectService {
  async createProject(data: ProjectInput): Promise<Project>
  async getAllProjects(): Promise<Project[]>
  async getProjectById(id: string): Promise<Project | null>
  async updateProject(id: string, updates: Partial<Project>): Promise<Project>
  async deleteProject(id: string, deleteTasks: boolean): Promise<void>
  async getProjectTasks(projectId: string): Promise<Task[]>
  async attachRule(projectId: string, ruleId: string): Promise<void>
  async detachRule(projectId: string, ruleId: string): Promise<void>
  async getProjectRules(projectId: string): Promise<Rule[]>
}
```

### EnhancedProjectService

```typescript
class EnhancedProjectService {
  async cloneProject(projectId: string, newName: string): Promise<Project>
  async validateRepositoryAccess(url: string): Promise<boolean>
  async getProjectStatistics(projectId: string): Promise<ProjectStats>
  async archiveProject(projectId: string): Promise<void>
  async restoreProject(projectId: string): Promise<void>
}
```

### GitProviderRegistry

```typescript
class GitProviderRegistry {
  registerProvider(provider: GitProvider): void
  getProvider(url: string): GitProvider | null
  validateUrl(url: string): boolean
  parseRepositoryInfo(url: string): RepositoryInfo
  
  // Supported providers
  providers: {
    github: GitHubProvider,
    gitlab: GitLabProvider,
    bitbucket: BitbucketProvider,
    custom: CustomGitProvider
  }
}
```

## Challenges and Solutions

### Challenge 1: Multi-Provider Git Support
**Problem**: Different Git providers (GitHub, GitLab, Bitbucket) have different URL formats and authentication methods.

**Solution**: Implement provider registry pattern
```typescript
class GitProviderRegistry {
  private providers: Map<string, GitProvider> = new Map();
  
  registerProvider(name: string, provider: GitProvider): void {
    this.providers.set(name, provider);
  }
  
  detectProvider(url: string): GitProvider | null {
    for (const provider of this.providers.values()) {
      if (provider.matchesUrl(url)) {
        return provider;
      }
    }
    return null;
  }
}

// Register providers
registry.registerProvider('github', new GitHubProvider());
registry.registerProvider('gitlab', new GitLabProvider());
registry.registerProvider('bitbucket', new BitbucketProvider());
```

### Challenge 2: Project Name Uniqueness
**Problem**: Multiple projects with same name cause confusion and conflicts.

**Solution**: Enforce unique constraints and provide validation
```typescript
async createProject(data: ProjectInput): Promise<Project> {
  // Check for duplicate name
  const existing = await this.getProjectByName(data.name);
  if (existing) {
    throw new Error(`Project "${data.name}" already exists`);
  }
  
  // Create project with unique ID
  const project = {
    id: uuidv4(),
    ...data,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };
  
  await this.db.insert('projects', project);
  return project;
}
```

### Challenge 3: Cascade Deletion
**Problem**: When deleting project, need to handle associated tasks, rules, and workspaces.

**Solution**: Implement transaction-based cascade deletion
```typescript
async deleteProject(id: string, deleteTasks: boolean): Promise<void> {
  return new Promise((resolve, reject) => {
    this.db.serialize(() => {
      this.db.run('BEGIN TRANSACTION');
      try {
        if (deleteTasks) {
          // Get all tasks for this project
          const tasks = await this.getProjectTasks(id);
          
          // Delete each task and workspace
          for (const task of tasks) {
            await taskService.deleteTask(task.id, true);
          }
        } else {
          // Set tasks to orphaned state
          this.db.run('UPDATE tasks SET projectId = NULL WHERE projectId = ?', [id]);
        }
        
        // Delete project-rule associations
        this.db.run('DELETE FROM project_rules WHERE projectId = ?', [id]);
        
        // Delete project
        this.db.run('DELETE FROM projects WHERE id = ?', [id]);
        
        this.db.run('COMMIT');
        resolve();
      } catch (error) {
        this.db.run('ROLLBACK');
        reject(error);
      }
    });
  });
}
```

### Challenge 4: Project Export/Import Versioning
**Problem**: Project export format may change over time, requiring version migration.

**Solution**: Version-aware export/import with migration
```typescript
interface ProjectExport {
  version: string;
  exportDate: Date;
  project: Project;
  rules: Rule[];
  tasks?: Task[];
  metadata: ExportMetadata;
}

class ProjectMigrationService {
  async migrateExport(exportData: any): Promise<ProjectExport> {
    const version = exportData.version || '1.0';
    
    switch (version) {
      case '1.0':
        return this.migrateFrom1_0To2_0(exportData);
      case '2.0':
        return exportData; // Current version
      default:
        throw new Error(`Unsupported export version: ${version}`);
    }
  }
  
  private async migrateFrom1_0To2_0(data: any): Promise<ProjectExport> {
    // Add new fields, transform data structures
    return {
      version: '2.0',
      exportDate: new Date(),
      project: {
        ...data.project,
        branchTemplate: data.project.branchTemplate || 'task-{taskId}',
        workspacePath: data.project.workspacePath || '~/.cursor-tasks'
      },
      rules: data.rules || [],
      metadata: {
        ...data.metadata,
        migratedFrom: '1.0'
      }
    };
  }
}
```

## What Else Can Be Added

### Immediate Enhancements

1. **Project Templates**
   - Pre-configured project templates
   - Technology-specific templates (React, Node.js, Python, etc.)
   - Custom template creation

2. **Project Tags**
   - Categorize projects with tags
   - Filter projects by tags
   - Tag-based analytics

3. **Project Favorites**
   - Mark frequently used projects as favorites
   - Quick access to favorite projects
   - Pin projects to top of list

4. **Project Archive**
   - Archive inactive projects
   - Restore archived projects
   - Auto-archive based on inactivity

5. **Repository Branch Management**
   - View repository branches
   - Switch default branch
   - Delete stale branches

### Medium-Term Enhancements

6. **Project Groups**
   - Organize projects into groups/folders
   - Hierarchical project structure
   - Group-level settings

7. **Project Sharing**
   - Share project configurations with team
   - Export for specific users
   - Collaborative project management

8. **CI/CD Integration**
   - Monitor build status
   - Trigger builds from app
   - View deployment history

9. **Project Dependencies**
   - Define dependencies between projects
   - Visualize dependency graph
   - Detect circular dependencies

10. **Advanced Repository Features**
    - Submodule support
    - Monorepo handling
    - Multiple repository association

## API Reference

### IPC Channels

```typescript
// Create project
ipcRenderer.invoke('create-project', data: ProjectInput): Promise<Project>

// Get all projects
ipcRenderer.invoke('get-all-projects'): Promise<Project[]>

// Get project by ID
ipcRenderer.invoke('get-project-by-id', id: string): Promise<Project | null>

// Update project
ipcRenderer.invoke('update-project', id: string, updates: Partial<Project>): Promise<Project>

// Delete project
ipcRenderer.invoke('delete-project', id: string, deleteTasks: boolean): Promise<void>

// Clone project
ipcRenderer.invoke('clone-project', id: string, newName: string): Promise<Project>

// Export project
ipcRenderer.invoke('export-project', id: string): Promise<string>

// Import project
ipcRenderer.invoke('import-project', exportPath: string): Promise<Project>

// Attach rule to project
ipcRenderer.invoke('attach-project-rule', projectId: string, ruleId: string): Promise<void>

// Detach rule from project
ipcRenderer.invoke('detach-project-rule', projectId: string, ruleId: string): Promise<void>

// Get project rules
ipcRenderer.invoke('get-project-rules', projectId: string): Promise<Rule[]>

// Get project tasks
ipcRenderer.invoke('get-project-tasks', projectId: string): Promise<Task[]>

// Validate repository URL
ipcRenderer.invoke('validate-repository-url', url: string): Promise<ValidationResult>
```

## Testing Strategy

### Unit Tests
```typescript
describe('ProjectService', () => {
  test('createProject - creates project successfully');
  test('createProject - rejects duplicate project name');
  test('getAllProjects - returns all projects');
  test('getProjectById - returns project if exists');
  test('updateProject - updates project fields');
  test('deleteProject - cascades to tasks');
  test('attachRule - associates rule with project');
});
```

### Integration Tests
- Project creation with Git operations
- Project deletion with cascade
- Import/export round-trip
- Multi-provider Git support

### E2E Tests
- Complete project creation and task association
- Project export and import workflow
- Project cloning with all settings

## Related Documentation

- [Task Management](02-task-management.md)
- [Git Integration](04-git-integration.md)
- [Rule Management](09-rule-management.md)
- [Import/Export](12-import-export.md)

