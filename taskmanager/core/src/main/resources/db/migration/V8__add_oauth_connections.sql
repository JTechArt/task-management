-- OAuth connections for external integrations (Story 4.2)
-- Tokens stored encrypted; metadata only in readable columns
CREATE TABLE oauth_connections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(200),
    team_id VARCHAR(200),
    scope VARCHAR(500),
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_provider CHECK (
        provider IN ('SLACK', 'GITHUB', 'GITLAB', 'BITBUCKET')
    )
);

CREATE INDEX idx_oauth_connections_provider ON oauth_connections(provider);
