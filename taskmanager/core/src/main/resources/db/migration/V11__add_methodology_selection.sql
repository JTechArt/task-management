ALTER TABLE projects
    ADD COLUMN methodology VARCHAR(50) NOT NULL DEFAULT 'NONE';

ALTER TABLE tasks
    ADD COLUMN methodology_override VARCHAR(50);
