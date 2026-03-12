-- Tasks table
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    workspace_path VARCHAR(1000),
    branch_name VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    CONSTRAINT valid_task_type CHECK (
        task_type IN ('FEATURE', 'BUG_FIX', 'RESEARCH', 'ENHANCEMENT', 'DOCUMENTATION', 'REFACTORING')
    ),
    CONSTRAINT valid_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')
    ),
    CONSTRAINT valid_title CHECK (length(title) > 0)
);

-- Indexes for tasks
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_type ON tasks(task_type);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);

-- Full-text search for tasks
CREATE INDEX idx_tasks_search ON tasks
    USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));

