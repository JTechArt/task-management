# IDE Integration Feature

## Overview
Direct integration with Cursor IDE, enabling one-click task launching with pre-configured workspace settings, rules, and context.

## What This Feature Does
- Launch Cursor IDE with task workspace
- Apply project rules to IDE
- Load task context automatically
- Configure IDE settings per task
- Multi-task IDE window management

## Acceptance Criteria

✅ **IDE Launching**
- Open Cursor with specific workspace path
- Apply project-specific rules
- Load task metadata as context

✅ **Context Management**
- Generate task context files
- Include task description and requirements
- Load relevant project documentation

✅ **Rule Application**
- Copy project rules to workspace .cursor/rules/
- Apply global rules
- Merge rule sets appropriately

## Technology Stack

**Service**: `IDEService.ts`

**Key Dependencies**:
```typescript
import { spawn } from 'child_process';
import * as fs from 'fs/promises';
import * as path from 'path';
```

## Key Methods

```typescript
class IDEService {
  async openTaskInIDE(taskId: string): Promise<void>
  async openProjectInIDE(projectId: string): Promise<void>
  async applyRulesToWorkspace(workspacePath: string, rules: Rule[]): Promise<void>
  async generateTaskContext(task: Task): Promise<string>
  async isIDEInstalled(): Promise<boolean>
}
```

## Implementation Flow

```
1. Resolve task workspace path
2. Load project rules from database
3. Copy rules to workspace .cursor/rules/
4. Generate task-context.md file
5. Build Cursor command with workspace path
6. Spawn Cursor process
7. Return immediately (non-blocking)
```

## IDE Launch Command

```typescript
// macOS
const cursorPath = '/Applications/Cursor.app/Contents/MacOS/Cursor';
spawn(cursorPath, [workspacePath], { detached: true });

// Windows
const cursorPath = 'C:\\Users\\<User>\\AppData\\Local\\Programs\\cursor\\Cursor.exe';
spawn(cursorPath, [workspacePath], { detached: true, shell: true });

// Linux
spawn('cursor', [workspacePath], { detached: true });
```

## Task Context File Format

```markdown
# Task: {Task Title}

## Description
{Task description}

## Type
{Task type}

## Project
{Project name}

## Repository
{Repository URL}

## Branch
{Branch name}

## Created
{Creation date}

## Requirements
- Requirement 1
- Requirement 2
```

## Challenges and Solutions

### Challenge: Cross-Platform IDE Paths
Cursor installation paths differ across OS.

**Solution**: Path detection with fallback strategies.

```typescript
async detectCursorPath(): Promise<string | null> {
  const possiblePaths = {
    darwin: ['/Applications/Cursor.app/Contents/MacOS/Cursor'],
    win32: [
      'C:\\Users\\' + process.env.USERNAME + '\\AppData\\Local\\Programs\\cursor\\Cursor.exe'
    ],
    linux: ['/usr/bin/cursor', '/usr/local/bin/cursor']
  };
  
  for (const path of possiblePaths[process.platform]) {
    if (await fs.pathExists(path)) {
      return path;
    }
  }
  return null;
}
```

### Challenge: Multiple IDE Windows
Managing multiple Cursor instances for different tasks.

**Solution**: Each task has isolated workspace; Cursor handles multiple windows.

## What Else Can Be Added
1. VS Code integration
2. JetBrains IDE support
3. Custom IDE profiles per task
4. IDE extension management
5. Remote IDE support

## API Reference

```typescript
ipcRenderer.invoke('open-task-in-ide', taskId: string): Promise<void>
ipcRenderer.invoke('open-project-in-ide', projectId: string): Promise<void>
ipcRenderer.invoke('check-ide-installed'): Promise<boolean>
```

## Related Documentation
- [Task Management](02-task-management.md)
- [Rule Management](09-rule-management.md)
