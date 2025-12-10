# Docker Setup for Kotlin/PostgreSQL Implementation

## Overview
Complete Docker configuration for developing and deploying AiTask with Kotlin backend and PostgreSQL database.

## Docker Compose Configuration

### docker-compose.yml (Development)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD:-devpassword}
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./database/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U aitask"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - aitask-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
      target: development
    container_name: aitask-backend
    ports:
      - "8080:8080"
      - "5005:5005"  # Debug port
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/aitask
      SPRING_DATASOURCE_USERNAME: aitask
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-devpassword}
      JAVA_TOOL_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    volumes:
      - ./backend/src:/app/src
      - gradle_cache:/root/.gradle
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - aitask-network
    command: ./gradlew bootRun --continuous

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      target: development
    container_name: aitask-frontend
    ports:
      - "3000:3000"
    environment:
      REACT_APP_API_URL: http://localhost:8080/api
      CHOKIDAR_USEPOLLING: true  # For hot reload
    volumes:
      - ./frontend/src:/app/src
      - ./frontend/public:/app/public
      - /app/node_modules
    networks:
      - aitask-network
    command: npm start

  adminer:
    image: adminer:latest
    container_name: aitask-adminer
    ports:
      - "8081:8080"
    environment:
      ADMINER_DEFAULT_SERVER: postgres
    networks:
      - aitask-network

volumes:
  postgres_data:
  gradle_cache:

networks:
  aitask-network:
    driver: bridge
```

### docker-compose.prod.yml (Production)

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: aitask-postgres-prod
    environment:
      POSTGRES_DB: aitask
      POSTGRES_USER: aitask
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - aitask-network

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
      target: production
    container_name: aitask-backend-prod
    expose:
      - "8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/aitask
      SPRING_DATASOURCE_USERNAME: aitask
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres
    restart: unless-stopped
    networks:
      - aitask-network
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
      target: production
    container_name: aitask-frontend-prod
    expose:
      - "80"
    depends_on:
      - backend
    restart: unless-stopped
    networks:
      - aitask-network

  nginx:
    image: nginx:alpine
    container_name: aitask-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - backend
      - frontend
    restart: unless-stopped
    networks:
      - aitask-network

volumes:
  postgres_prod_data:

networks:
  aitask-network:
    driver: bridge
```

## Backend Dockerfile

```dockerfile
# backend/Dockerfile

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build application
RUN ./gradlew bootJar --no-daemon

# Development stage
FROM eclipse-temurin:21-jdk-alpine AS development

WORKDIR /app

COPY --from=build /app/gradle gradle
COPY --from=build /app/gradlew .
COPY --from=build /app/build.gradle.kts .
COPY --from=build /app/settings.gradle.kts .

# Create volume mount points
VOLUME /app/src

EXPOSE 8080 5005

CMD ["./gradlew", "bootRun", "--continuous"]

# Production stage
FROM eclipse-temurin:21-jre-alpine AS production

WORKDIR /app

# Copy built JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
```

## Frontend Dockerfile

```dockerfile
# frontend/Dockerfile

# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source
COPY . .

# Build for production
RUN npm run build

# Development stage
FROM node:20-alpine AS development

WORKDIR /app

COPY package*.json ./
RUN npm install

# Create volume mount points
VOLUME /app/src
VOLUME /app/public

EXPOSE 3000

CMD ["npm", "start"]

# Production stage
FROM nginx:alpine AS production

# Copy built files
COPY --from=build /app/build /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

## Nginx Configuration

```nginx
# nginx/nginx.conf

upstream backend {
    server backend:8080;
}

upstream frontend {
    server frontend:80;
}

server {
    listen 80;
    server_name localhost;

    # Frontend
    location / {
        proxy_pass http://frontend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend API
    location /api/ {
        proxy_pass http://backend/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass http://backend/actuator/health;
    }
}
```

## Environment Variables

### .env (Development)
```env
# Database
DB_PASSWORD=devpassword

# Backend
SPRING_PROFILES_ACTIVE=dev
JWT_SECRET=dev-secret-key-change-in-production

# Frontend
REACT_APP_API_URL=http://localhost:8080/api
```

### .env.prod (Production)
```env
# Database
DB_PASSWORD=strong-production-password

# Backend
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=very-secure-production-secret-key

# Frontend
REACT_APP_API_URL=https://api.aitask.com
```

## Usage Commands

### Development

```bash
# Start all services
docker-compose up

# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild services
docker-compose up --build

# Stop and remove volumes
docker-compose down -v
```

### Production

```bash
# Start production stack
docker-compose -f docker-compose.prod.yml up -d

# View production logs
docker-compose -f docker-compose.prod.yml logs -f backend

# Update services
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# Backup database
docker exec aitask-postgres-prod pg_dump -U aitask aitask > backup.sql
```

### Individual Service Management

```bash
# Rebuild backend only
docker-compose up --build backend

# View backend logs
docker-compose logs -f backend

# Access database
docker-compose exec postgres psql -U aitask

# Run backend tests
docker-compose exec backend ./gradlew test

# Access backend shell
docker-compose exec backend sh
```

## Docker Best Practices

1. **Multi-stage builds**: Separate build and runtime stages
2. **Layer caching**: Order COPY commands for optimal caching
3. **Non-root user**: Run as non-root in production
4. **Health checks**: Add health checks for all services
5. **Resource limits**: Set memory and CPU limits
6. **Volumes**: Use volumes for persistent data
7. **Networks**: Isolate services with custom networks
8. **.dockerignore**: Exclude unnecessary files

## Troubleshooting

### Database Connection Issues
```bash
# Check if postgres is healthy
docker-compose ps

# View postgres logs
docker-compose logs postgres

# Test connection
docker-compose exec postgres pg_isready -U aitask
```

### Build Failures
```bash
# Clean and rebuild
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Performance Issues
```bash
# Check resource usage
docker stats

# Increase resources in Docker Desktop settings
```

See [10-deployment-guide.md](10-deployment-guide.md) for production deployment strategies.
