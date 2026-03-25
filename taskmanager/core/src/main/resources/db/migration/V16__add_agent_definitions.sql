CREATE TABLE agent_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    prompt_template TEXT NOT NULL,
    llm_configuration_id UUID REFERENCES llm_configurations(id) ON DELETE SET NULL,
    scope VARCHAR(50) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    trigger VARCHAR(50) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_agent_scope CHECK (scope IN ('GLOBAL', 'PROJECT')),
    CONSTRAINT valid_agent_trigger CHECK (trigger IN ('MANUAL', 'TASK_OPENED', 'TASK_UPDATED')),
    CONSTRAINT valid_project_scope CHECK (
        (scope = 'GLOBAL' AND project_id IS NULL)
        OR (scope = 'PROJECT' AND project_id IS NOT NULL)
    )
);

CREATE INDEX idx_agent_definitions_scope ON agent_definitions(scope);
CREATE INDEX idx_agent_definitions_project ON agent_definitions(project_id);
CREATE INDEX idx_agent_definitions_enabled ON agent_definitions(is_enabled) WHERE is_enabled = TRUE;
