Main Project Goal

AiTask is a standalone desktop task management application designed for developers to manage development tasks efficiently. The application automates
workspace setup, integrates with Git repositories, provides task tracking, and enables AI-assisted development through Cursor IDE integration.

Project Vision: Create a modern, cross-platform desktop application that transforms how developers manage their development workflow by combining task
management, Git operations, workspace automation, and AI-powered development assistance.

Core Features



1. Project Management
•  Multi-project workspace organization
•  Repository(s) management with Git provider support (GitHub, GitLab, Bitbucket)
    - project might have multiple connected repositories (mfes)
    - For each repo (project module) IDE(s) must be specified , ex: for typescipt project webstorm, cursor ai or vscode might be selected. 
    - For each repo AI product(s) must be specified , ex: for some project cursor ai might be preferable so the rules would be combined for cursor AI for aughment  appropriatre rules would be copied 
•  Custom workspace paths per project
•  Branch naming templates (e.g., task-{taskId})
•  Project metadata storage (name, URL, description, team(s), tag(s))
•  Project import/export functionality
•  Project lifecycle management

2. Task Management
•  Create, read, update, and delete development tasks
•  Automatic workspace generation for each task
•  Git repository cloning and branch creation (depth 1 to exclude git history)
•  Task status tracking (Pending → In Progress → Completed → Archived)
•  Task type support (Feature, Bug Fix, Research, Enhancement, Documentation, Refactoring)
•  IDE integration 
    - for launching tasks in Cursor IDE
    - for launching tasks in VSCode
    - for launching tasks Jet Brience / PyCharm, WebStorm , IntelIJ
•  Workspace cleanup on task completion/deletion

3. Git Integration
•  Clone repositories from multiple Git providers
•  Create task-specific branches
•  Commit and push operations
•  Support for GitHub, GitLab, and Bitbucket
    - automatically apply ~/.ssh/config configuration as different accoutns requires different SSH keys
    - support HTTPS as well. (username/password will be stored encrupted in the database)
•  Automatic credential management
•  Repository validation and URL parsing

4. Rule Management
•  Global and project-specific custom rules
•  Attach rules to projects
•  Import/export rules with projects
•  Share rules across projects
•  Cursor AI, VSCOde, Augment rule integration


5. Dashboard
•  Task overview and analytics
•  Project statistics
•  Recent activity feed
•  Task completion metrics
•  Time-based filtering and search

6. Slack Integration
•  Send notifications to Slack channels
•  Channel management within projects
•  Real-time updates on task changes
•  Integration with Slack API for notifications

7. Authentication
•  Generic OAuth2 framework
•  Support for multiple authentication providers
•  Secure credential storage
•  Git credential helper integration

8. Import/Export
•  JSON-based data portability
•  Export/import projects, tasks, and rules
•  Data migration between installations
•  Backup and restore functionality

9. Connection Monitoring
•  Service health monitoring
•  Database connection status
•  Git repository availability checks
•  External API connectivity monitoring

10. IDE Integration
•  Launch selected IDEA (Cursor IDE or VSCode or JetBriense product)  with task context (open button should suggest the available IDEA to open project)
•  Auto-configure IDE with project rules
•  Load task context into development environment
•  Support for multiple concurrent IDE windows

Technology Stack Summary

Build System
•  Gradle 8.5+: Modern build system with superior Kotlin DSL support, better performance, and comprehensive dependency management for Kotlin projects

Application Layer
•  Kotlin 2.0+: Modern JVM language with null safety, coroutines
•  JetBrains Compose Multiplatform for Desktop 1.6+: Declarative UI framework for cross-platform desktop development (Windows, macOS, Linux)
•  Material Design 3: Modern UI component library with beautiful, accessible components
•  Kotlinx Coroutines 1.8+: Structured concurrency for async operations

Data Layer
•  PostgreSQL 16+: Enterprise-grade relational database with ACID compliance, JSON support, full-text search, and excellent scalability
•  Exposed ORM 0.50+: Type-safe SQL DSL for Kotlin with transaction support and DAO pattern
•  HikariCP 5.1+: High-performance JDBC connection pooling
•  Flyway 10+: Database migration management with version control

Integration Layer
•  JGit 6.9+: Pure Java Git implementation for repository operations
•  Ktor Client 2.3+: Async HTTP client for Slack API and external service integration
•  kotlinx.serialization 1.6+: JSON serialization with type-safe encoding

Testing Stack
•  JUnit 5: Modern testing framework
•  MockK 1.13+: Kotlin mocking framework
•  Kotlinx Coroutines Test: Testing coroutine-based code

Infrastructure
•  Docker Compose: Multi-container orchestration for PostgreSQL and application
•  JPackage: Native installer generation for Windows, macOS, and Linux
•  PostgreSQL Docker Image: Containerized database deployment
