-- Add retention_policy to projects for workspace cleanup behavior
ALTER TABLE projects ADD COLUMN IF NOT EXISTS retention_policy VARCHAR(50) NOT NULL DEFAULT 'KEEP_ALL';

-- Add workspace_cleaned_at to tasks to track cleanup outcome
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS workspace_cleaned_at TIMESTAMP;
