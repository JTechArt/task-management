# Environment Setup Guide - Maven/Kotlin/PostgreSQL Stack

## Overview
This document outlines prerequisites and environment setup for developing and running the AiTask standalone application built with Maven, Kotlin, Jetpack Compose Desktop, and PostgreSQL.

## System Requirements

### Operating Systems
- **macOS**: 11.0 (Big Sur) or later
- **Windows**: Windows 10 (64-bit) or Windows 11
- **Linux**: Ubuntu 20.04+, Fedora 35+, or equivalent

### Hardware Requirements
- **CPU**: Multi-core processor (Intel i5/AMD Ryzen 5 or better recommended)
- **RAM**: Minimum 8GB, 16GB recommended for development
- **Disk Space**: At least 5GB free space (JDK, Maven, Docker, PostgreSQL)
- **Display**: 1920x1080 recommended for development

## Required Software

### 1. JDK 21 (LTS)
**Version**: OpenJDK 21 or Oracle JDK 21

**Installation via SDKMAN (Recommended)**:
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install JDK 21
sdk install java 21.0.2-open

# Set as default
sdk default java 21.0.2-open

# Verify installation
java -version
# Output: openjdk version "21.0.2" ...
```

**macOS (using Homebrew)**:
```bash
brew install openjdk@21

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify
java -version
```

**Windows (using Installer)**:
1. Download from https://jdk.java.net/21/
2. Run installer and follow prompts
3. Set `JAVA_HOME` environment variable:
   ```powershell
   setx JAVA_HOME "C:\Program Files\Java\jdk-21"
   setx PATH "%PATH%;%JAVA_HOME%\bin"
   ```
4. Verify: `java -version`

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install openjdk-21-jdk

# Verify
java -version
javac -version
```

**Verification**:
```bash
java -version   # Should show 21.x.x
javac -version  # Should show 21.x.x
echo $JAVA_HOME # Should show JDK 21 path
```

### 2. Maven 3.9+
**Version**: Apache Maven 3.9.6 or later

**Installation via SDKMAN (Recommended)**:
```bash
# Install Maven
sdk install maven 3.9.6

# Set as default
sdk default maven 3.9.6

# Verify
mvn -version
# Output: Apache Maven 3.9.6 ...
```

**macOS (using Homebrew)**:
```bash
brew install maven

# Verify
mvn -version
```

**Windows (Manual Installation)**:
1. Download from https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add to PATH:
   ```powershell
   setx M2_HOME "C:\Program Files\Apache\maven"
   setx PATH "%PATH%;%M2_HOME%\bin"
   ```
4. Verify: `mvn -version`

**Linux (Ubuntu/Debian)**:
```bash
sudo apt update
sudo apt install maven

# Verify
mvn -version
```

**Verification**:
```bash
mvn -version
# Should show:
# Apache Maven 3.9.6
# Maven home: ...
# Java version: 21.0.2
```

### 3. Docker Desktop
**Version**: Docker Desktop 4.25+ with Docker Compose

**macOS Installation**:
```bash
# Using Homebrew
brew install --cask docker

# Or download from https://www.docker.com/products/docker-desktop/
```

**Windows Installation**:
1. Download Docker Desktop from https://www.docker.com/products/docker-desktop/
2. Run installer
3. Enable WSL 2 backend (recommended)
4. Start Docker Desktop

**Linux Installation (Ubuntu)**:
```bash
# Install Docker Engine
sudo apt update
sudo apt install ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Add user to docker group
sudo usermod -aG docker $USER
newgrp docker
```

**Verification**:
```bash
docker --version
# Output: Docker version 24.0.x ...

docker-compose version
# Output: Docker Compose version v2.23.x ...

# Test Docker
docker run hello-world
```

### 4. Git 2.x+
**Version**: Git 2.30 or later

**macOS**:
```bash
brew install git
```

**Windows**:
- Download from https://git-scm.com/download/win
- Run installer with default options

**Linux**:
```bash
sudo apt install git
```

**Verification**:
```bash
git --version
# Output: git version 2.40.x or higher
```

### 5. IDE (Recommended)

**IntelliJ IDEA (Recommended for Kotlin)**:
```bash
# macOS
brew install --cask intellij-idea-ce  # Community Edition
# or
brew install --cask intellij-idea     # Ultimate Edition

# Download from: https://www.jetbrains.com/idea/download/
```

**Required Plugins**:
- Kotlin (bundled)
- Compose Multiplatform (bundled with latest version)
- Database Tools (bundled in Ultimate)
- Docker (bundled in Ultimate)

**Alternative: VS Code**:
```bash
# macOS
brew install --cask visual-studio-code

# Required Extensions:
# - Kotlin Language
# - Maven for Java
# - Docker
# - PostgreSQL
```

## Project Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd aitask-kotlin
```

### 2. Verify Project Structure
```bash
# Should see:
ls -la
# pom.xml
# docker-compose.yml
# src/
# README.md
```

### 3. Install Maven Dependencies
```bash
# Download all dependencies
mvn dependency:resolve

# Verify installation
mvn dependency:tree
```

### 4. Compile Project
```bash
# Clean and compile
mvn clean compile

# Should complete without errors
```

## Database Setup

### 1. Start PostgreSQL with Docker Compose
```bash
# Start PostgreSQL container
docker-compose up -d postgres

# Check PostgreSQL is running
docker-compose ps

# View logs
docker-compose logs postgres

# Expected output:
# "database system is ready to accept connections"
```

### 2. Verify Database Connection
```bash
# Connect to PostgreSQL
docker exec -it aitask-postgres psql -U aitask -d aitask

# Inside psql:
# aitask=# \dt         (list tables - should be empty initially)
# aitask=# \q          (quit)
```

### 3. Run Database Migrations
```bash
# Run Flyway migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Expected output:
# | Version | Description      | State   |
# | 1       | initial schema   | Success |
# | 2       | add indexes      | Success |
```

### 4. Verify Schema Created
```bash
# Connect to database
docker exec -it aitask-postgres psql -U aitask -d aitask

# List tables
\dt

# Expected tables:
# - projects
# - tasks
# - rules
# - project_rules
# - slack_channels
# - flyway_schema_history

# Describe a table
\d tasks

# Exit
\q
```

## Configuration Files

### 1. Application Configuration

Create `src/main/resources/application.yaml`:
```yaml
database:
  host: ${DB_HOST:localhost}
  port: ${DB_PORT:5432}
  name: ${DB_NAME:aitask}
  user: ${DB_USER:aitask}
  password: ${DB_PASSWORD:aitask_secret}
  pool:
    maximumPoolSize: 10
    minimumIdle: 5
    connectionTimeout: 30000

logging:
  level:
    com.aitask: INFO
    org.jetbrains.exposed: DEBUG

workspace:
  basePath: ${WORKSPACE_PATH:./workspaces}
  
ide:
  cursorPath: ${CURSOR_PATH:/Applications/Cursor.app/Contents/MacOS/Cursor}
```

### 2. Environment Variables

Create `.env` file in project root:
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=aitask
DB_USER=aitask
DB_PASSWORD=aitask_secret

# Application
WORKSPACE_PATH=./workspaces
CURSOR_PATH=/Applications/Cursor.app/Contents/MacOS/Cursor

# Slack (Optional)
SLACK_BOT_TOKEN=xoxb-your-bot-token
SLACK_SIGNING_SECRET=your-signing-secret

# Encryption
ENCRYPTION_KEY=your-32-character-encryption-key
```

**Load environment variables**:
```bash
# In your shell startup file (.bashrc, .zshrc)
export $(grep -v '^#' .env | xargs)

# Or use direnv
brew install direnv
# Add to .envrc: source .env
direnv allow
```

### 3. Git Configuration (Optional)

Configure Git credentials helper:
```bash
# macOS Keychain
git config --global credential.helper osxkeychain

# Linux
git config --global credential.helper cache

# Windows
git config --global credential.helper wincred
```

## Development Tools

### 1. Database Client (Optional)

**DBeaver (Free, Cross-platform)**:
```bash
# macOS
brew install --cask dbeaver-community

# Connection settings:
# Host: localhost
# Port: 5432
# Database: aitask
# Username: aitask
# Password: aitask_secret
```

**pgAdmin (Web-based)**:
```bash
# Run in Docker
docker run -d \
  --name pgadmin \
  -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=admin@aitask.com \
  -e PGADMIN_DEFAULT_PASSWORD=admin \
  dpage/pgadmin4

# Access at http://localhost:5050
```

### 2. API Testing Tools

**HTTPie (CLI)**:
```bash
brew install httpie

# Test Slack API
http POST https://slack.com/api/chat.postMessage \
  Authorization:"Bearer $SLACK_BOT_TOKEN" \
  channel=C123456 \
  text="Hello from AiTask"
```

**Postman (GUI)**:
- Download from https://www.postman.com/downloads/

### 3. Docker Management

**Portainer (Web UI for Docker)**:
```bash
docker volume create portainer_data
docker run -d \
  --name portainer \
  -p 9000:9000 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce

# Access at http://localhost:9000
```

## Running the Application

### 1. Development Mode
```bash
# Start database
docker-compose up -d postgres

# Run application with hot reload
mvn compile compose:run

# Or with exec plugin
mvn compile exec:java -Dexec.mainClass="com.aitask.AitaskApplicationKt"
```

### 2. Production Build
```bash
# Create JAR
mvn clean package

# Run JAR
java -jar target/aitask-kotlin-1.0.0.jar
```

### 3. Docker Deployment
```bash
# Build and run entire stack
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

### 4. Native Installer
```bash
# Create native installer for current OS
mvn clean package jpackage:jpackage

# Output in target/dist/
# - macOS: AiTask-1.0.0.dmg
# - Windows: AiTask-1.0.0.msi
# - Linux: aitask-1.0.0.deb
```

## Troubleshooting

### Issue: JDK not found
**Solution**:
```bash
# Check JAVA_HOME
echo $JAVA_HOME

# Set JAVA_HOME if empty
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux

# Add to shell profile
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
```

### Issue: Maven dependencies fail to download
**Solution**:
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Re-download dependencies
mvn clean install -U

# Use Maven mirror if needed
# Add to ~/.m2/settings.xml:
<mirrors>
  <mirror>
    <id>central-mirror</id>
    <url>https://repo.maven.apache.org/maven2</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>
```

### Issue: Docker permission denied
**Solution**:
```bash
# Linux: Add user to docker group
sudo usermod -aG docker $USER
newgrp docker

# macOS/Windows: Ensure Docker Desktop is running
```

### Issue: PostgreSQL container won't start
**Solution**:
```bash
# Check if port 5432 is in use
lsof -i :5432

# Stop existing PostgreSQL
brew services stop postgresql  # macOS
sudo systemctl stop postgresql  # Linux

# Or change port in docker-compose.yml:
ports:
  - "5433:5432"  # Use 5433 instead
```

### Issue: Database migration fails
**Solution**:
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Reset database (WARNING: Deletes all data)
docker-compose down -v
docker-compose up -d postgres
mvn flyway:clean
mvn flyway:migrate
```

### Issue: Compose Desktop UI not rendering
**Solution**:
```bash
# Ensure display is set (Linux)
export DISPLAY=:0

# Install required libraries (Linux)
sudo apt install libx11-6 libxrender1 libxtst6 libxi6

# Check Java graphics
java -Dsun.java2d.trace=log -jar target/aitask-*.jar
```

### Issue: Out of memory during build
**Solution**:
```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m"

# Or add to pom.xml:
<properties>
  <maven.compiler.fork>true</maven.compiler.fork>
  <maven.compiler.meminitial>1024m</maven.compiler.meminitial>
  <maven.compiler.maxmem>2048m</maven.compiler.maxmem>
</properties>
```

## Verification Checklist

Before proceeding to development, ensure:

- [ ] JDK 21 installed and `java -version` shows 21.x
- [ ] Maven 3.9+ installed and `mvn -version` works
- [ ] Docker Desktop running and `docker ps` works
- [ ] Git 2.x+ installed and configured
- [ ] Repository cloned successfully
- [ ] Maven dependencies resolved (`mvn dependency:resolve`)
- [ ] Project compiles without errors (`mvn compile`)
- [ ] PostgreSQL container running (`docker-compose ps`)
- [ ] Database migrations applied (`mvn flyway:info`)
- [ ] Configuration files created (application.yaml, .env)
- [ ] IDE installed with Kotlin plugin
- [ ] Application runs (`mvn compose:run`)

## Environment Variables Reference

```bash
# Required
JAVA_HOME=/path/to/jdk-21
DB_HOST=localhost
DB_PORT=5432
DB_NAME=aitask
DB_USER=aitask
DB_PASSWORD=aitask_secret

# Optional
WORKSPACE_PATH=./workspaces
CURSOR_PATH=/path/to/cursor
SLACK_BOT_TOKEN=xoxb-...
ENCRYPTION_KEY=32-character-key
LOG_LEVEL=INFO
```

## Next Steps

After completing environment setup:

1. **Review Architecture** - [14-architecture-overview.md](14-architecture-overview.md)
2. **Study Technology Stack** - [01-technology-stack.md](01-technology-stack.md)
3. **Understand Database Schema** - [03-database-design.md](03-database-design.md)
4. **Run and Debug** - [01-run-and-debug.md](01-run-and-debug.md)
5. **Start Feature Development** - [02-task-management.md](02-task-management.md)

## Additional Resources

### Documentation
- **Kotlin**: https://kotlinlang.org/docs/home.html
- **Maven**: https://maven.apache.org/guides/
- **Compose Desktop**: https://www.jetbrains.com/lp/compose-desktop/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Docker**: https://docs.docker.com/

### Tools
- **SDKMAN**: https://sdkman.io/
- **IntelliJ IDEA**: https://www.jetbrains.com/idea/
- **DBeaver**: https://dbeaver.io/
- **Postman**: https://www.postman.com/

### Community
- **Kotlin Slack**: https://surveys.jetbrains.com/s3/kotlin-slack-sign-up
- **Compose Desktop GitHub**: https://github.com/JetBrains/compose-multiplatform
- **Stack Overflow**: Tag questions with `kotlin`, `compose-desktop`, `maven`

## Support

For environment setup issues:
1. Check troubleshooting section above
2. Review application logs: `target/logs/aitask.log`
3. Check Docker logs: `docker-compose logs`
4. Verify database: `docker exec -it aitask-postgres psql -U aitask`
5. Consult [Run and Debug Guide](01-run-and-debug.md)
