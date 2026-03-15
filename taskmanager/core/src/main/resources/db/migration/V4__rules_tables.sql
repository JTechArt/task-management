-- Rules table
CREATE TABLE rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    scope VARCHAR(50) NOT NULL,
    target_ide VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP,
    
    CONSTRAINT valid_scope CHECK (
        scope IN ('GLOBAL', 'PROJECT', 'REPOSITORY', 'IDE')
    ),
    CONSTRAINT valid_category CHECK (
        category IS NULL OR category IN ('CODING_STANDARDS', 'ARCHITECTURE', 'TESTING', 'DOCUMENTATION', 'AI_ASSISTANT')
    ),
    CONSTRAINT valid_target_ide CHECK (
        target_ide IS NULL OR target_ide IN ('CURSOR', 'VSCODE', 'INTELLIJ', 'PYCHARM', 'WEBSTORM', 'GOLAND', 'ANDROID_STUDIO', 'XCODE', 'SUBLIME', 'VIM', 'EMACS', 'OTHER')
    )
);

-- Project Rules junction table
CREATE TABLE project_rules (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    rule_id UUID NOT NULL REFERENCES rules(id) ON DELETE CASCADE,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, rule_id)
);

-- Indexes for rules
CREATE INDEX idx_rules_scope ON rules(scope);
CREATE INDEX idx_rules_category ON rules(category) WHERE category IS NOT NULL;
CREATE INDEX idx_rules_target_ide ON rules(target_ide) WHERE target_ide IS NOT NULL;
CREATE INDEX idx_rules_archived ON rules(archived_at) WHERE archived_at IS NOT NULL;
CREATE INDEX idx_rules_name ON rules(name);

-- Indexes for project_rules
CREATE INDEX idx_project_rules_project ON project_rules(project_id);
CREATE INDEX idx_project_rules_rule ON project_rules(rule_id);

-- Full-text search for rules
CREATE INDEX idx_rules_search ON rules
    USING gin(to_tsvector('english', name || ' ' || content));

