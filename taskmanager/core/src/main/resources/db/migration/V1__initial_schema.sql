-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Projects table
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    workspace_path VARCHAR(1000) NOT NULL,
    branch_template VARCHAR(200) DEFAULT 'task-{taskId}',
    tags TEXT[],
    team VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    
    CONSTRAINT valid_workspace_path CHECK (length(workspace_path) > 0)
);

-- Repositories table
CREATE TABLE repositories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    clone_url VARCHAR(500) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    auth_type VARCHAR(50) NOT NULL,
    preferred_ides TEXT[],
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_clone_url CHECK (
        clone_url ~* '^(https?|git|ssh)://.*'
    ),
    CONSTRAINT valid_provider CHECK (
        provider IN ('GITHUB', 'GITLAB', 'BITBUCKET', 'OTHER')
    ),
    CONSTRAINT valid_auth_type CHECK (
        auth_type IN ('SSH', 'HTTPS', 'TOKEN')
    )
);

-- Indexes for projects
CREATE INDEX idx_projects_name ON projects(name);
CREATE INDEX idx_projects_created_at ON projects(created_at DESC);
CREATE INDEX idx_projects_archived ON projects(archived_at) WHERE archived_at IS NOT NULL;

-- Indexes for repositories
CREATE INDEX idx_repositories_project ON repositories(project_id);
CREATE INDEX idx_repositories_provider ON repositories(provider);
CREATE INDEX idx_repositories_primary ON repositories(is_primary) WHERE is_primary = TRUE;

-- Full-text search for projects
CREATE INDEX idx_projects_search ON projects
    USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));

