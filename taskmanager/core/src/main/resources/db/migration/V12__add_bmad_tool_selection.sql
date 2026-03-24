ALTER TABLE projects
    ADD COLUMN bmad_tool_ids TEXT[];

ALTER TABLE tasks
    ADD COLUMN bmad_tool_override_ids TEXT[];
