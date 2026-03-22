CREATE TABLE pre_run_scripts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    repository_id UUID REFERENCES repositories(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    script_path VARCHAR(1000),
    inline_script TEXT,
    required_value VARCHAR(200),
    execution_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pre_run_scripts_target_check CHECK (
        project_id IS NOT NULL
    ),
    CONSTRAINT pre_run_scripts_payload_check CHECK (
        (type IN ('INLINE_COMMAND') AND inline_script IS NOT NULL) OR
        (type IN ('SCRIPT_PATH') AND script_path IS NOT NULL) OR
        (type IN ('NODE_VERSION', 'JAVA_VERSION', 'PYTHON_VERSION', 'ENVIRONMENT_VARIABLE') AND required_value IS NOT NULL)
    )
);

CREATE INDEX idx_pre_run_scripts_project ON pre_run_scripts(project_id);
CREATE INDEX idx_pre_run_scripts_repository ON pre_run_scripts(repository_id);
CREATE INDEX idx_pre_run_scripts_order ON pre_run_scripts(project_id, execution_order, created_at);
