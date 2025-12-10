# Rule Management Feature

## Overview
Manage Cursor AI rules for projects, including custom rule creation, dynamic rule generation, rule templates, and project-specific rule associations.

## What This Feature Does
- Create and manage AI assistant rules
- Associate rules with projects
- Generate rules dynamically from prompts
- Rule templates for common scenarios
- Import/export rules
- Global vs project-specific rules

## Acceptance Criteria

✅ **Rule CRUD**
- Create rules with name, content, category
- Update rule content
- Delete rules (with cascade handling)
- List all rules and filter by category

✅ **Rule Association**
- Attach rules to projects
- Detach rules from projects
- Global rules applied to all projects

✅ **Dynamic Generation**
- Generate rules from natural language prompts
- Use templates for common patterns
- AI-assisted rule creation

## Technology Stack

**Services**:
- `RuleService.ts` - Core rule management
- `DynamicRuleService.ts` - AI-powered generation
- `FileService.ts` - Rule file I/O

**Database Schema**:
```sql
CREATE TABLE rules (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT,
    isGlobal INTEGER DEFAULT 0,
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
```

## Key Methods

```typescript
class RuleService {
  async createRule(data: RuleInput): Promise<Rule>
  async getAllRules(): Promise<Rule[]>
  async getRuleById(id: string): Promise<Rule | null>
  async updateRule(id: string, updates: Partial<Rule>): Promise<Rule>
  async deleteRule(id: string): Promise<void>
  async attachToProject(ruleId: string, projectId: string): Promise<void>
  async getProjectRules(projectId: string): Promise<Rule[]>
}

class DynamicRuleService {
  async generateRule(prompt: string, context?: RuleContext): Promise<GeneratedRule>
  async generateFromTemplate(templateId: string, variables: Record<string, any>): Promise<GeneratedRule>
}
```

## Rule Format

Rules are stored as Markdown (.mdc) files:

```markdown
# Rule Name

## Description
Brief description of what this rule does.

## Guidelines
- Guideline 1
- Guideline 2

## Examples
```typescript
// Example code
```
\`\`\`
```

## Challenges and Solutions

### Challenge: Rule File Synchronization
Rules in database must stay in sync with .mdc files in .cursor/rules/.

**Solution**: Two-way sync with file watcher and database triggers.

### Challenge: Rule Conflicts
Multiple rules might contradict each other.

**Solution**: Rule precedence system (project rules override global rules).

## What Else Can Be Added
1. Rule validation and linting
2. Rule versioning and history
3. Rule marketplace/sharing
4. Conflict detection and resolution
5. Rule effectiveness analytics

## API Reference

```typescript
ipcRenderer.invoke('create-rule', data: RuleInput): Promise<Rule>
ipcRenderer.invoke('get-all-rules'): Promise<Rule[]>
ipcRenderer.invoke('attach-rule-to-project', ruleId: string, projectId: string): Promise<void>
ipcRenderer.invoke('generate-dynamic-rule', prompt: string): Promise<GeneratedRule>
```

## Related Documentation
- [Project Management](03-project-management.md)
- [Visual Mockup Generation](08-visual-mockup-generation.md)
- [IDE Integration](10-ide-integration.md)
