CREATE TABLE saved_prompts (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    scope VARCHAR(50) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_prompt_scope CHECK (scope IN ('GLOBAL', 'PROJECT')),
    CONSTRAINT valid_prompt_project_scope CHECK (
        (scope = 'GLOBAL' AND project_id IS NULL)
        OR (scope = 'PROJECT' AND project_id IS NOT NULL)
    )
);

CREATE INDEX idx_saved_prompts_scope ON saved_prompts(scope);
CREATE INDEX idx_saved_prompts_project ON saved_prompts(project_id);
