# Git Integration Feature

## Overview
Git Integration provides comprehensive Git operations including repository cloning, branch management, and multi-provider support for GitHub, GitLab, Bitbucket, and custom Git servers.

## What This Feature Does
- Clone Git repositories from various providers
- Create and manage Git branches automatically
- Parse and validate Git URLs (HTTPS, SSH, Git protocol)
- Support multiple authentication methods
- Handle Git operations with error recovery
- Integrate with task and project workflows

## Acceptance Criteria

### Functional Requirements
✅ **Repository Operations**
- Clone repositories via HTTPS, SSH, or Git protocol
- Validate repository URLs before operations
- Handle authentication (SSH keys, tokens, credentials)
- Retry failed operations with exponential backoff

✅ **Branch Management**
- Create branches from default or specified base
- Generate branch names from templates
- Switch between branches
- Delete branches (local and remote)

✅ **Multi-Provider Support**
- GitHub (github.com, GitHub Enterprise)
- GitLab (gitlab.com, self-hosted)
- Bitbucket (bitbucket.org, Bitbucket Server)
- Custom Git servers
- Local file system repositories

✅ **URL Format Support**
- HTTPS: `https://github.com/user/repo.git`
- SSH: `git@github.com:user/repo.git`
- Git Protocol: `git://github.com/user/repo.git`
- Local: `file:///path/to/repo`

### Non-Functional Requirements
✅ **Reliability**
- Retry failed operations (max 3 attempts)
- Clean up partial clones on failure
- Handle network timeouts gracefully

✅ **Performance**
- Shallow clones for faster operations
- Parallel clone operations support
- Progress reporting for long operations

## Technology Stack

### Backend Service

**Primary Service**: `GitService.ts`
```typescript
Location: src/main/services/GitService.ts
Language: TypeScript 5.2+
Library: simple-git 3.28+
```

**Dependencies**:
```typescript
import simpleGit, { SimpleGit, CloneOptions } from 'simple-git';
```

### Git Provider Registry

**Service**: `GitProviderRegistry.ts`
```typescript
Location: src/main/services/GitProviderRegistry.ts
Pattern: Provider Registry Pattern
```

**Utility**: `GitUrlParser.ts`
```typescript
Location: src/main/utils/GitUrlParser.ts
Purpose: URL parsing and validation
```

## Implementation Architecture

### Repository Clone Flow

```
User/Service Request
    ↓
GitService.cloneRepository()
    ↓
┌──────────────────────────────────┐
│ 1. Validate URL format           │
│ 2. Detect Git provider           │
│ 3. Check destination exists      │
│ 4. Initialize simple-git         │
│ 5. Execute clone with options    │
│ 6. Retry on failure (max 3x)     │
│ 7. Clean up on final failure     │
│ 8. Verify clone success          │
└──────────────────────────────────┘
    ↓
Return success or throw error
```

### Branch Creation Flow

```
Task Creation Request
    ↓
GitService.createBranch()
    ↓
┌──────────────────────────────────┐
│ 1. Navigate to repository        │
│ 2. Fetch latest changes          │
│ 3. Generate branch name          │
│ 4. Check branch doesn't exist    │
│ 5. Create branch from base       │
│ 6. Checkout new branch           │
│ 7. Push to remote (optional)     │
└──────────────────────────────────┘
    ↓
Return branch name
```

## Key Classes and Methods

### GitService

```typescript
class GitService {
  private git: SimpleGit;
  
  // Repository operations
  async cloneRepository(
    url: string,
    destination: string,
    options?: CloneOptions
  ): Promise<void>
  
  async validateRepository(url: string): Promise<boolean>
  
  async getRepositoryInfo(path: string): Promise<RepoInfo>
  
  // Branch operations
  async createBranch(
    repoPath: string,
    branchName: string,
    baseBranch?: string
  ): Promise<string>
  
  async switchBranch(
    repoPath: string,
    branchName: string
  ): Promise<void>
  
  async deleteBranch(
    repoPath: string,
    branchName: string,
    force?: boolean
  ): Promise<void>
  
  async getCurrentBranch(repoPath: string): Promise<string>
  
  async listBranches(repoPath: string): Promise<string[]>
  
  // Remote operations
  async fetch(repoPath: string): Promise<void>
  
  async pull(repoPath: string): Promise<void>
  
  async push(
    repoPath: string,
    remote?: string,
    branch?: string
  ): Promise<void>
}
```

### GitProviderRegistry

```typescript
class GitProviderRegistry {
  private static instance: GitProviderRegistry;
  private providers: Map<string, GitProvider>;
  
  registerProvider(provider: GitProvider): void
  
  getProvider(url: string): GitProvider | null
  
  detectProvider(url: string): string | null
  
  // Built-in providers
  getGitHubProvider(): GitHubProvider
  getGitLabProvider(): GitLabProvider
  getBitbucketProvider(): BitbucketProvider
}

interface GitProvider {
  name: string;
  matchesUrl(url: string): boolean;
  parseUrl(url: string): RepositoryInfo;
  getCloneUrl(info: RepositoryInfo, protocol: 'https' | 'ssh'): string;
  supportsFeature(feature: string): boolean;
}
```

### GitUrlParser

```typescript
class GitUrlParser {
  static parse(url: string): ParsedGitUrl | null
  
  static isValidGitUrl(url: string): boolean
  
  static normalizeUrl(url: string): string
  
  static extractRepoName(url: string): string
  
  static convertToHttps(url: string): string
  
  static convertToSsh(url: string): string
}

interface ParsedGitUrl {
  protocol: 'https' | 'ssh' | 'git' | 'file';
  host: string;
  owner: string;
  repo: string;
  fullPath: string;
}
```

## Challenges and Solutions

### Challenge 1: URL Format Variations
**Problem**: Git URLs come in many formats and need unified handling.

**Solution**: Comprehensive URL parser with regex patterns
```typescript
class GitUrlParser {
  private static readonly PATTERNS = {
    // HTTPS: https://github.com/user/repo.git
    https: /^https?:\/\/([^/]+)\/(.+?)(?:\.git)?$/,
    
    // SSH: git@github.com:user/repo.git
    ssh: /^git@([^:]+):(.+?)(?:\.git)?$/,
    
    // Git Protocol: git://github.com/user/repo.git
    git: /^git:\/\/([^/]+)\/(.+?)(?:\.git)?$/,
    
    // File: file:///path/to/repo
    file: /^file:\/\/(.+)$/
  };
  
  static parse(url: string): ParsedGitUrl | null {
    for (const [protocol, pattern] of Object.entries(this.PATTERNS)) {
      const match = url.match(pattern);
      if (match) {
        return this.buildParsedUrl(protocol, match);
      }
    }
    return null;
  }
}
```

### Challenge 2: Authentication Handling
**Problem**: Different providers and protocols require different authentication.

**Solution**: Provider-specific authentication strategies
```typescript
interface GitAuthStrategy {
  type: 'ssh-key' | 'token' | 'credentials' | 'none';
  configure(git: SimpleGit): void;
}

class SSHKeyAuth implements GitAuthStrategy {
  type = 'ssh-key' as const;
  
  configure(git: SimpleGit): void {
    // SSH keys are handled by system SSH agent
    // No explicit configuration needed
  }
}

class TokenAuth implements GitAuthStrategy {
  type = 'token' as const;
  constructor(private token: string) {}
  
  configure(git: SimpleGit): void {
    git.addConfig('credential.helper', 'store');
    // Token is embedded in URL: https://token@github.com/user/repo.git
  }
}

class CredentialsAuth implements GitAuthStrategy {
  type = 'credentials' as const;
  constructor(
    private username: string,
    private password: string
  ) {}
  
  configure(git: SimpleGit): void {
    // Credentials embedded in URL or use credential helper
  }
}
```

### Challenge 3: Clone Failures and Cleanup
**Problem**: Failed clones can leave partial repositories that cause issues.

**Solution**: Retry with exponential backoff and cleanup
```typescript
async cloneRepository(
  url: string,
  destination: string,
  options?: CloneOptions
): Promise<void> {
  const maxRetries = 3;
  const baseDelay = 1000; // 1 second
  
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      // Check if destination already has partial clone
      if (await fs.pathExists(destination)) {
        await fs.rm(destination, { recursive: true, force: true });
      }
      
      await this.git.clone(url, destination, options);
      
      // Verify clone was successful
      const isValid = await this.validateRepository(destination);
      if (!isValid) {
        throw new Error('Cloned repository is invalid');
      }
      
      logger.info('GitService', `Successfully cloned: ${url}`);
      return;
      
    } catch (error) {
      logger.warn('GitService', `Clone attempt ${attempt + 1} failed: ${error.message}`);
      
      // Clean up partial clone
      if (await fs.pathExists(destination)) {
        await fs.rm(destination, { recursive: true, force: true });
      }
      
      if (attempt < maxRetries - 1) {
        // Exponential backoff
        const delay = baseDelay * Math.pow(2, attempt);
        await sleep(delay);
      } else {
        throw new Error(
          `Failed to clone repository after ${maxRetries} attempts: ${error.message}`
        );
      }
    }
  }
}
```

### Challenge 4: Branch Name Conflicts
**Problem**: Generated branch names might already exist in repository.

**Solution**: Check for existence and append suffix if needed
```typescript
async createUniqueBranch(
  repoPath: string,
  baseName: string,
  baseFrom?: string
): Promise<string> {
  const git = simpleGit(repoPath);
  const branches = await git.branchLocal();
  
  let branchName = baseName;
  let suffix = 1;
  
  // Keep trying until we find a unique name
  while (branches.all.includes(branchName)) {
    branchName = `${baseName}-${suffix}`;
    suffix++;
  }
  
  await git.checkoutBranch(branchName, baseFrom || 'HEAD');
  
  logger.info('GitService', `Created branch: ${branchName}`);
  return branchName;
}
```

### Challenge 5: Large Repository Performance
**Problem**: Cloning large repositories can be very slow.

**Solution**: Use shallow clones and optimization options
```typescript
async cloneRepositoryFast(
  url: string,
  destination: string
): Promise<void> {
  const options: CloneOptions = {
    '--depth': 1,              // Shallow clone (only latest commit)
    '--single-branch': null,   // Only clone default branch
    '--no-tags': null,         // Skip tags
    '--filter': 'blob:none'    // Skip large blobs initially
  };
  
  await this.cloneRepository(url, destination, options);
  
  logger.info('GitService', 'Fast clone completed');
}
```

## What Else Can Be Added

### Immediate Enhancements

1. **Git Credential Manager**
   - Store and manage Git credentials securely
   - Support for SSH key management
   - OAuth token storage

2. **Repository Health Check**
   - Verify repository integrity
   - Check for uncommitted changes
   - Detect repository issues

3. **Advanced Branch Operations**
   - Merge branches
   - Rebase operations
   - Cherry-pick commits

4. **Git Hooks Support**
   - Pre-commit hooks
   - Post-commit hooks
   - Custom hook management

5. **Submodule Support**
   - Clone repositories with submodules
   - Update submodules
   - Manage submodule configurations

### Medium-Term Enhancements

6. **Git LFS Support**
   - Handle large files with Git LFS
   - Configure LFS settings
   - Track LFS files

7. **Commit Operations**
   - Stage and commit files
   - View commit history
   - Amend commits

8. **Diff and Comparison**
   - View file diffs
   - Compare branches
   - Show uncommitted changes

9. **Stash Management**
   - Stash uncommitted changes
   - Apply stashed changes
   - List and manage stashes

10. **Tag Management**
    - Create tags
    - List tags
    - Delete tags
    - Push tags to remote

## API Reference

### IPC Channels

```typescript
// Clone repository
ipcRenderer.invoke('git-clone', url: string, destination: string): Promise<void>

// Create branch
ipcRenderer.invoke('git-create-branch', repoPath: string, branchName: string, base?: string): Promise<string>

// Switch branch
ipcRenderer.invoke('git-switch-branch', repoPath: string, branchName: string): Promise<void>

// Get current branch
ipcRenderer.invoke('git-current-branch', repoPath: string): Promise<string>

// List branches
ipcRenderer.invoke('git-list-branches', repoPath: string): Promise<string[]>

// Validate URL
ipcRenderer.invoke('git-validate-url', url: string): Promise<boolean>

// Parse URL
ipcRenderer.invoke('git-parse-url', url: string): Promise<ParsedGitUrl>

// Fetch updates
ipcRenderer.invoke('git-fetch', repoPath: string): Promise<void>

// Pull changes
ipcRenderer.invoke('git-pull', repoPath: string): Promise<void>

// Push changes
ipcRenderer.invoke('git-push', repoPath: string, remote?: string, branch?: string): Promise<void>
```

## Testing Strategy

### Unit Tests
```typescript
describe('GitService', () => {
  test('cloneRepository - clones valid repository');
  test('cloneRepository - handles invalid URL');
  test('cloneRepository - retries on failure');
  test('createBranch - creates new branch');
  test('createBranch - handles existing branch');
});

describe('GitUrlParser', () => {
  test('parse - parses HTTPS URL');
  test('parse - parses SSH URL');
  test('parse - parses Git protocol URL');
  test('isValidGitUrl - validates URL formats');
});
```

### Integration Tests
- Clone from GitHub, GitLab, Bitbucket
- Branch creation and switching
- Authentication with different methods

### E2E Tests
- Complete task creation with git clone
- Repository validation workflow
- Multi-provider support

## Related Documentation

- [Project Management](03-project-management.md)
- [Task Management](02-task-management.md)
- [Architecture Overview](14-architecture-overview.md)

