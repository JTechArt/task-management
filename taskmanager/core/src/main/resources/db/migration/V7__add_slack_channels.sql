-- Slack channel configuration per project (Incoming Webhook integration for Story 4.1)
CREATE TABLE slack_channels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_url VARCHAR(500) NOT NULL,
    channel_display_name VARCHAR(200) NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    enabled_events TEXT DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_webhook_url CHECK (
        webhook_url ~* '^https://hooks\.slack\.com/services/.*'
    )
);

CREATE INDEX idx_slack_channels_project ON slack_channels(project_id);
