ALTER TABLE agent_definitions ADD COLUMN associated_tools VARCHAR(500);

UPDATE agent_definitions SET associated_tools = 'LOCAL_LLM' WHERE associated_tools IS NULL;
