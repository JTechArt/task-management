CREATE TABLE llm_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    endpoint_url VARCHAR(1000) NOT NULL,
    model_identifier VARCHAR(200) NOT NULL,
    api_key_encrypted TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_configurations_default
    ON llm_configurations(is_default);
