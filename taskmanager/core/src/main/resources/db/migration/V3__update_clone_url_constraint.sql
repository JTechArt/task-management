-- Update clone_url constraint to accept SSH shorthand format (git@github.com:user/repo.git)
ALTER TABLE repositories DROP CONSTRAINT IF EXISTS valid_clone_url;

ALTER TABLE repositories ADD CONSTRAINT valid_clone_url CHECK (
    clone_url ~* '^(https?|git|ssh)://.*' OR clone_url ~* '^[a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+:.*'
);

