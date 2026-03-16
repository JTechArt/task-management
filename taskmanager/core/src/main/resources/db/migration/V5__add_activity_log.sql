-- Activity Log table for tracking user actions and system events
CREATE TABLE activity_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activity_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    user_id VARCHAR(100),
    description TEXT NOT NULL,
    metadata TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS'
);

-- Indexes for activity log
CREATE INDEX idx_activity_timestamp ON activity_log(created_at DESC);
CREATE INDEX idx_activity_entity ON activity_log(entity_type, entity_id);
CREATE INDEX idx_activity_type ON activity_log(activity_type);
CREATE INDEX idx_activity_project ON activity_log(project_id) WHERE project_id IS NOT NULL;
CREATE INDEX idx_activity_status ON activity_log(status);
