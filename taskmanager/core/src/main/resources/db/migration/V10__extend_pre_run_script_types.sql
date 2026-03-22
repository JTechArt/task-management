ALTER TABLE pre_run_scripts
    DROP CONSTRAINT IF EXISTS pre_run_scripts_payload_check;

ALTER TABLE pre_run_scripts
    ADD CONSTRAINT pre_run_scripts_payload_check CHECK (
        (type IN ('INLINE_COMMAND') AND inline_script IS NOT NULL) OR
        (type IN ('SCRIPT_PATH') AND script_path IS NOT NULL) OR
        (type IN ('NODE_VERSION', 'JAVA_VERSION', 'PYTHON_VERSION', 'ENVIRONMENT_VARIABLE', 'DEPENDENCY_PRESENT') AND required_value IS NOT NULL)
    );
