# Import/Export Feature

## Overview
Project and task data import/export functionality for backup, migration, and sharing configurations between installations.

## What This Feature Does
- Export projects with all settings
- Export tasks with workspace metadata
- Import projects from JSON files
- Import tasks and restore workspaces
- Migrate data between versions

## Acceptance Criteria

✅ **Project Export**
- Export project configuration
- Include associated rules
- Package task references
- Generate timestamped JSON

✅ **Project Import**
- Parse and validate JSON
- Migrate data format if needed
- Restore rules and settings
- Handle conflicts gracefully

✅ **Task Export/Import**
- Export task metadata
- Preserve workspace references
- Import with workspace recreation option

## Technology Stack

**Services**:
- `ProjectExportService.ts` - Export logic
- `ProjectImportService.ts` - Import logic
- `ProjectMigrationService.ts` - Version migration

## Export Format

```json
{
  "version": "2.0",
  "exportDate": "2025-01-10T12:00:00Z",
  "project": {
    "id": "project-123",
    "name": "My Project",
    "repositoryUrl": "https://github.com/user/repo",
    "description": "Project description",
    "branchTemplate": "task-{taskId}",
    "createdAt": 1704902400000
  },
  "rules": [
    {
      "id": "rule-456",
      "name": "TypeScript Rules",
      "content": "# Rules content...",
      "category": "coding-standards"
    }
  ],
  "tasks": [
    {
      "id": "task-789",
      "title": "Feature X",
      "description": "Implement feature X",
      "taskType": "feature",
      "status": "in_progress"
    }
  ],
  "metadata": {
    "exportedBy": "user@example.com",
    "appVersion": "1.0.0"
  }
}
```

## Key Methods

```typescript
class ProjectExportService {
  async exportProject(projectId: string): Promise<string>
  async exportToFile(projectId: string, filePath: string): Promise<void>
  async exportMultipleProjects(projectIds: string[]): Promise<string>
}

class ProjectImportService {
  async importProject(exportData: string): Promise<Project>
  async importFromFile(filePath: string): Promise<Project>
  async validateImport(exportData: string): Promise<ValidationResult>
}

class ProjectMigrationService {
  async migrateExport(exportData: any): Promise<ProjectExport>
  async detectVersion(exportData: any): string
  async migrateFromV1ToV2(data: any): Promise<ProjectExport>
}
```

## Implementation Flow

**Export**:
```
1. Retrieve project from database
2. Fetch associated rules
3. Fetch associated tasks (optional)
4. Package into export object
5. Add metadata (version, timestamp, etc.)
6. Serialize to JSON
7. Save to file or return string
```

**Import**:
```
1. Parse JSON file
2. Detect export version
3. Migrate if needed
4. Validate data structure
5. Check for conflicts (duplicate names)
6. Create/update project in database
7. Import associated rules
8. Optionally restore tasks
9. Return imported project
```

## Challenges and Solutions

### Challenge: Version Compatibility
Old exports may use different data format.

**Solution**: Version detection and migration pipeline.

### Challenge: Duplicate Handling
Imported project name may conflict with existing.

**Solution**: Prompt user or auto-rename with suffix.

```typescript
async resolveNameConflict(name: string): Promise<string> {
  let finalName = name;
  let suffix = 1;
  
  while (await this.projectExists(finalName)) {
    finalName = `${name} (${suffix})`;
    suffix++;
  }
  
  return finalName;
}
```

## What Else Can Be Added
1. Selective export (choose what to include)
2. Batch import/export
3. Cloud backup integration
4. Encrypted exports for sensitive data
5. Import from other tools (Jira, Asana)

## API Reference

```typescript
ipcRenderer.invoke('export-project', projectId: string): Promise<string>
ipcRenderer.invoke('export-project-to-file', projectId: string, filePath: string): Promise<void>
ipcRenderer.invoke('import-project', exportPath: string): Promise<Project>
ipcRenderer.invoke('validate-import', exportPath: string): Promise<ValidationResult>
```

## Related Documentation
- [Project Management](03-project-management.md)
- [Task Management](02-task-management.md)
