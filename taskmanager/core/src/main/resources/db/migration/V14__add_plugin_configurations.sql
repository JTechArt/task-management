CREATE TABLE plugin_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plugin_id VARCHAR(200) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    scope_key VARCHAR(200),
    active_configuration_json TEXT NOT NULL,
    last_known_good_configuration_json TEXT,
    validation_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    validation_message TEXT,
    last_validated_at BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT plugin_configurations_scope_check CHECK (scope IN ('APP', 'PROJECT'))
);

CREATE UNIQUE INDEX idx_plugin_configurations_scope_unique
    ON plugin_configurations(plugin_id, scope, scope_key);

CREATE INDEX idx_plugin_configurations_plugin
    ON plugin_configurations(plugin_id);
