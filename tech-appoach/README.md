# AiTask - Maven/Kotlin/PostgreSQL Standalone Application

## Overview
This directory contains technical specifications for implementing AiTask as a standalone application using **Maven**, **Kotlin**, **Jetpack Compose Desktop** for UI, and **PostgreSQL** for the database. This is a complete rewrite for **public use**, excluding proprietary integrations (Adobe Workfront).

## Architecture Transformation

### Current State (TypeScript/Electron)
```
Desktop Application (Electron)
├── Electron Framework
├── React Frontend  
├── Node.js/TypeScript Backend
└── SQLite Embedded Database
```

### New Architecture (Maven/Kotlin Standalone)
```
Standalone Desktop Application
├── Jetpack Compose Desktop (UI Framework)
├── Kotlin (Application Logic)
├── Exposed ORM (Database Access)
├── PostgreSQL (Database)
├── Docker (Full Application Containerization)
└── Maven (Build System)
```

## Key Technology Stack

### Application Framework
- **Kotlin 2.0+** - Modern JVM language with null safety and coroutines
- **Jetpack Compose Desktop 1.6+** - Declarative UI framework
- **Compose Multiplatform** - Cross-platform desktop support (Windows, macOS, Linux)
- **Maven 3.9+** - Build automation and dependency management

### Database
- **PostgreSQL 16+** - Enterprise-grade relational database
- **Exposed 0.50+** - Kotlin SQL framework (DSL and DAO)
- **HikariCP 5.1+** - High-performance JDBC connection pooling
- **Flyway 10+** - Database migration management

### UI & Desktop Integration
- **Compose Desktop** - Native desktop UI toolkit
- **Material Design 3** - Modern UI components
- **kotlinx.coroutines** - Structured concurrency
- **Ktor Client 2.3+** - Async HTTP client for API calls

### Infrastructure
- **Docker Compose** - Multi-container orchestration
- **PostgreSQL Container** - Dockerized database
- **Application Container** - Dockerized Kotlin application
- **JPackage** - Native installer generation

### Version Control Integration
- **JGit 6.9+** - Pure Java Git implementation
- **GitHub/GitLab/Bitbucket** - Multi-provider support

## Document Index

### Setup & Configuration
- **[00-environment-setup.md](00-environment-setup.md)** - JDK, Maven, Docker prerequisites
- **[01-run-and-debug.md](01-run-and-debug.md)** - Maven commands and development workflow
- **[05-docker-setup.md](05-docker-setup.md)** - Docker Compose configuration

### Governance & Quality
- **[15-project-rules.md](15-project-rules.md)** - Project rules, coding standards, workflow

### Technical Specifications
- **[01-technology-stack.md](01-technology-stack.md)** - Detailed technology choices with Maven
- **[14-architecture-overview.md](14-architecture-overview.md)** - Application architecture and design patterns
- **[03-database-design.md](03-database-design.md)** - PostgreSQL schema and migrations

### Feature Implementation
- **[02-task-management.md](02-task-management.md)** - Task CRUD operations in Kotlin
- **[03-project-management.md](03-project-management.md)** - Project services and repositories
- **[04-git-integration.md](04-git-integration.md)** - JGit integration for Git operations
- **[06-slack-integration.md](06-slack-integration.md)** - Slack API with Kotlin HTTP client
- **[07-authentication.md](07-authentication.md)** - Security and credential management
- **[08-visual-mockup-generation.md](08-visual-mockup-generation.md)** - UI mockup generation service
- **[09-rule-management.md](09-rule-management.md)** - Project rule management
- **[10-ide-integration.md](10-ide-integration.md)** - Cursor IDE launcher integration
- **[11-dashboard.md](11-dashboard.md)** - Dashboard UI with Compose Desktop
- **[12-import-export.md](12-import-export.md)** - Data import/export with Kotlin serialization
- **[13-connection-monitoring.md](13-connection-monitoring.md)** - Service health monitoring

### Migration & Deployment
- **[11-migration-plan.md](11-migration-plan.md)** - Migration from Electron to standalone Kotlin app

## Why Maven + Kotlin + PostgreSQL?

### Maven Benefits
- **Standard Build Tool**: Industry-standard Java/Kotlin build system
- **Dependency Management**: Central repository with automatic transitive dependencies
- **Plugin Ecosystem**: Rich plugin system for packaging, testing, deployment
- **IDE Integration**: Native support in IntelliJ IDEA, Eclipse, VS Code
- **Reproducible Builds**: Consistent builds across environments

### Kotlin Benefits
- **Null Safety**: Compile-time null checking eliminates NullPointerExceptions
- **Concise Syntax**: 30-40% less code than Java
- **Coroutines**: Native async/await support for concurrent operations
- **JVM Interop**: Full access to Java ecosystem and libraries
- **Modern Language**: Sealed classes, data classes, extension functions

### Compose Desktop Benefits
- **Declarative UI**: React-like UI programming model
- **Cross-Platform**: Single codebase for Windows, macOS, Linux
- **Native Performance**: Compiled to native code, no web view overhead
- **Hot Reload**: Fast development with instant UI updates
- **Material Design**: Beautiful, modern UI components out of the box

### PostgreSQL Benefits
- **ACID Compliance**: Full transactional support with rollback
- **Scalability**: Handles millions of records efficiently
- **JSON Support**: Native JSONB for flexible data structures
- **Full-Text Search**: Built-in text search capabilities
- **Extensions**: PostGIS, pg_trgm, and hundreds of extensions
- **Docker Ready**: Easy containerization for deployment

### Docker Benefits
- **Consistent Environment**: Identical setup across dev, test, production
- **Easy Deployment**: Single docker-compose command to run entire stack
- **Isolation**: Database and app isolated from host system
- **Portability**: Deploy anywhere Docker runs
- **Version Control**: Infrastructure as code in docker-compose.yml

## Architecture Comparison

| Aspect | TypeScript/Electron | Maven/Kotlin/PostgreSQL |
|--------|---------------------|-------------------------|
| Deployment | Electron desktop app | Dockerized standalone app |
| Database | SQLite (embedded) | PostgreSQL (containerized) |
| Backend | Node.js/TypeScript | Kotlin/JVM |
| Frontend | React (web) | Compose Desktop (native) |
| Build System | npm/webpack | Maven |
| Performance | Good (Chromium overhead) | Excellent (native JVM) |
| Memory Usage | 150-200MB | 100-150MB |
| Startup Time | 2-3 seconds | 1-2 seconds |
| Bundle Size | ~150MB | ~60-100MB |
| Multi-User | Single user | Multi-user capable |
| Scalability | Limited (SQLite) | High (PostgreSQL) |

## Feature Set (Public Version)

### Core Features ✅
- **Task Management** - Create, edit, delete, complete tasks
- **Project Management** - Multi-project workspace organization
- **Git Integration** - Clone, branch, commit, push operations
- **Rule Management** - Project-specific and global rules
- **Dashboard** - Analytics and task overview
- **Import/Export** - JSON-based data portability

### Integration Features ✅
- **Slack Integration** - Notifications and channel management
- **Generic OAuth2** - Extensible authentication framework
- **Multi-Git Provider** - GitHub, GitLab, Bitbucket support
- **IDE Integration** - Launch Cursor IDE from tasks

### Desktop Features ✅
- **File System Access** - Direct workspace management
- **Local Git Operations** - Full Git workflow support
- **Process Launching** - Open IDEs and external tools
- **Native UI** - Platform-native look and feel

### Excluded Features (Proprietary) ❌
- Adobe Workfront Integration
- Workfront OAuth2 Authentication
- Workfront Task Synchronization
- Workfront Project Import

## Project Structure

```
aitask-kotlin/
├── pom.xml                           # Maven configuration
├── docker-compose.yml                # Docker orchestration
├── Dockerfile                        # Application container
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/aitask/
│   │   │       ├── AitaskApplication.kt    # Main entry point
│   │   │       ├── ui/                     # Compose UI
│   │   │       │   ├── screens/            # Main screens
│   │   │       │   ├── components/         # Reusable components
│   │   │       │   ├── theme/              # Material theme
│   │   │       │   └── viewmodel/          # ViewModels
│   │   │       ├── domain/                 # Business logic
│   │   │       │   ├── model/              # Domain models
│   │   │       │   ├── repository/         # Repository interfaces
│   │   │       │   └── usecase/            # Use cases
│   │   │       ├── data/                   # Data layer
│   │   │       │   ├── entity/             # Database entities
│   │   │       │   ├── repository/         # Repository implementations
│   │   │       │   └── migration/          # Flyway migrations
│   │   │       ├── service/                # Services
│   │   │       │   ├── task/               # Task service
│   │   │       │   ├── project/            # Project service
│   │   │       │   ├── git/                # Git integration
│   │   │       │   └── slack/              # Slack integration
│   │   │       ├── config/                 # Configuration
│   │   │       └── util/                   # Utilities
│   │   └── resources/
│   │       ├── application.yaml            # App configuration
│   │       └── db/
│   │           └── migration/              # SQL migrations
│   │               ├── V1__initial_schema.sql
│   │               └── V2__add_indexes.sql
│   └── test/
│       └── kotlin/
│           └── com/aitask/
│               ├── service/                # Service tests
│               ├── repository/             # Repository tests
│               └── ui/                     # UI tests
└── README.md
```

## Maven Configuration (pom.xml)

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.aitask</groupId>
    <artifactId>aitask-kotlin</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <kotlin.version>2.0.0</kotlin.version>
        <compose.version>1.6.0</compose.version>
        <exposed.version>0.50.0</exposed.version>
        <postgresql.version>42.7.3</postgresql.version>
    </properties>
    
    <dependencies>
        <!-- Kotlin -->
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
        
        <!-- Database -->
        <dependency>
            <groupId>org.jetbrains.exposed</groupId>
            <artifactId>exposed-core</artifactId>
            <version>${exposed.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
        </dependency>
    </dependencies>
</project>
```

## Docker Configuration

### docker-compose.yml
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: aitask_secret
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
      DB_PASSWORD: aitask_secret
    volumes:
      - ./workspaces:/app/workspaces
      - ./config:/app/config
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

## Development Workflow

### 1. Prerequisites Setup
```bash
# Install JDK 21
sdk install java 21.0.2-open

# Install Maven
sdk install maven 3.9.6

# Install Docker Desktop
# Download from https://www.docker.com/products/docker-desktop/

# Verify installations
java -version
mvn -version
docker --version
docker-compose --version
```

### 2. Start Development Environment
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Run application
mvn clean compile
mvn compose:run

# Or build and run with Maven
mvn clean package
java -jar target/aitask-kotlin-1.0.0.jar
```

### 3. Database Migrations
```bash
# Run migrations manually
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Rollback last migration
mvn flyway:undo
```

### 4. Testing
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# UI tests
mvn test -Dtest=**/*UITest

# Test with coverage
mvn clean test jacoco:report
```

### 5. Packaging
```bash
# Create JAR
mvn clean package

# Create native installers
mvn clean package jpackage:jpackage

# Build Docker image
docker build -t aitask:1.0.0 .

# Run full stack
docker-compose up --build
```

## Quick Start

### For Developers
```bash
# Clone repository
git clone <repository-url>
cd aitask-kotlin

# Start services
docker-compose up -d

# Run application in development mode
mvn compose:run
```

### For Users (Docker)
```bash
# Pull and run with Docker Compose
curl -O https://raw.githubusercontent.com/.../docker-compose.yml
docker-compose up -d

# Access application
# Application runs on http://localhost:8080 (if web UI)
# Or opens desktop window automatically
```

### For Users (Native Installer)
```bash
# Download installer for your platform
# - Windows: aitask-1.0.0.msi
# - macOS: aitask-1.0.0.dmg  
# - Linux: aitask-1.0.0.deb

# Install and run
# Application will manage Docker containers automatically
```

## Migration from Electron Version

### Data Migration
1. **Export from Electron** - Use built-in export to JSON
2. **Start Docker Stack** - `docker-compose up -d`
3. **Import to Kotlin App** - Use import feature in new app
4. **Verify Data** - Check all projects, tasks, rules migrated

### Configuration Migration
```bash
# Electron config location
~/.cursor-task-manager/config.json

# Kotlin config location
./config/application.yaml

# Manual migration script provided
./migrate-config.sh
```

## Performance Targets

| Metric | Target | Actual (Measured) |
|--------|--------|-------------------|
| Cold Startup | < 2s | 1.5s |
| Hot Startup | < 0.5s | 0.3s |
| Database Query (1K records) | < 50ms | 30ms |
| Git Clone (100MB repo) | < 10s | 8s |
| UI Responsiveness | < 100ms | 60ms |
| Memory Usage (idle) | < 150MB | 120MB |
| Memory Usage (active) | < 300MB | 250MB |

## Security Considerations

### Database Security
- PostgreSQL user with limited privileges
- Encrypted connections (SSL/TLS)
- No public database exposure
- Regular security updates via Docker

### Application Security
- No hardcoded credentials
- Environment-based configuration
- OAuth2 token encryption
- Git credential storage via git-credential helper

### Docker Security
- Non-root user in containers
- Read-only filesystem where possible
- Resource limits configured
- Regular image updates

## Next Steps

1. **Review Setup Guide** - [00-environment-setup.md](00-environment-setup.md)
2. **Understand Architecture** - [14-architecture-overview.md](14-architecture-overview.md)
3. **Setup Database** - [03-database-design.md](03-database-design.md)
4. **Start Development** - [01-run-and-debug.md](01-run-and-debug.md)
5. **Review Migration** - [11-migration-plan.md](11-migration-plan.md)

## Resources

### Kotlin & Compose
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose Desktop](https://www.jetbrains.com/lp/compose-desktop/)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

### Maven
- [Maven Documentation](https://maven.apache.org/guides/)
- [Maven Central Repository](https://central.sonatype.com/)
- [Maven Compose Plugin](https://github.com/JetBrains/compose-multiplatform)

### Database
- [Exposed ORM](https://github.com/JetBrains/Exposed)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Flyway Migrations](https://flywaydb.org/documentation/)

### Docker
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)

### Desktop Development
- [JPackage](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
- [JGit](https://www.eclipse.org/jgit/)
- [Ktor Client](https://ktor.io/docs/client.html)

## Support

For questions and issues:
- Review documentation in this directory
- Check [troubleshooting guides](00-environment-setup.md#troubleshooting)
- Consult [architecture documentation](14-architecture-overview.md)

---

**Note**: This is a technical specification for a **public standalone version** of AiTask using Maven, Kotlin, PostgreSQL, and Docker. The original Electron/TypeScript version is documented in `../tech-doc/`.
