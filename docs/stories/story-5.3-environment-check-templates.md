# Story 5.3: Environment Check Templates

**Epic:** Epic 5 - Pre-Run Scripts and Environment Validation

**As a** developer,  
**I want** templates for common environment checks (runtime versions, env vars),  
**so that** I can quickly validate Node.js, Java, Python, or required variables without writing custom scripts.

## Status

Done

## Acceptance Criteria

1. The system supports environment checks within pre-run scripts for Node.js, Java, and Python versions.
2. The system supports checking required environment variables.
3. The application provides templates for common checks (runtime versions, env vars, dependency presence).
4. Users can add template-based checks to their project with minimal configuration (e.g., target version, variable name).
5. Template checks produce clear pass/fail output with actionable messages when validation fails.

## Requirements Mapping

- PRE-3: Environment checks (Node, Java, Python, env vars)
- PRE-5: Templates for common checks

## Architecture References

- [Component Architecture: Workspace Management](../architecture.md#6-workspace-management-component)

## UX References

- [Front-end Spec: Preflight Scripts & Environment Validation](../front-end-spec.md#11-preflight-scripts--environment-validation)
- [Mockup: Preflight](../mockups/preflight.html)

## Tasks / Subtasks

- [x] Task 1: Define environment check template model and registry
  - [x] Create EnvironmentCheckTemplate (id, name, script generator, parameters)
  - [x] Support: node_version, java_version, python_version, env_var, dependency_present
- [x] Task 2: Implement template-to-script generation
  - [x] Generate executable script from template + user params
  - [x] Cross-platform: shell for macOS/Linux, batch for Windows
- [x] Task 3: Add template selection UI in pre-run config
  - [x] Template picker with parameter inputs
  - [x] Preview generated script before save
- [x] Task 4: Add unit tests for template generation and check logic

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew :core:test --tests 'com.aitask.core.domain.model.EnvironmentCheckTemplateRegistryTest' --tests 'com.aitask.core.infrastructure.prerun.EnvironmentTemplateExecutionTest' --tests 'com.aitask.core.infrastructure.prerun.LocalPreRunScriptServiceTest'`
- `./gradlew :desktop-app:compileKotlin`
- `./gradlew test`

### Completion Notes List

- Added a reusable environment check template registry covering Node.js, Java, Python, environment-variable, and dependency-presence checks.
- Extended pre-run script support with `DEPENDENCY_PRESENT` and a follow-up migration so template-based checks persist correctly.
- Updated the local pre-run executor to generate clearer, cross-platform commands for template-backed checks with more actionable failure output.
- Added template selection and generated-script preview to the Pre-Run dialog so users can add common checks with minimal input.
- Added unit coverage for the template registry and dependency-presence execution flow.

### File List

- `docs/stories/story-5.3-environment-check-templates.md`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/EnvironmentCheckTemplate.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/PreRunScript.kt`
- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
- `taskmanager/core/src/main/resources/db/migration/V10__extend_pre_run_script_types.sql`
- `taskmanager/core/src/test/kotlin/com/aitask/core/domain/model/EnvironmentCheckTemplateRegistryTest.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/EnvironmentTemplateExecutionTest.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`

### Change Log

- 2026-03-22: Added reusable environment-check templates, generated preview support, dependency-presence checks, and tests for story 5.3.

## QA Results

### Review Date: 2026-03-22

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation meets all acceptance criteria. EnvironmentCheckTemplateRegistry provides five templates (Node.js, Java, Python, environment variable, dependency presence) with cross-platform script generation (Unix vs Windows). PreRunScriptDialog includes template picker, parameter inputs, and generated-script preview. Failure messages are actionable (e.g., "Node.js 20 is required. Install it or switch versions.").

### Refactoring Performed

- **File**: `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
  - **Change**: Added regex validation for template parameter values before interpolation into shell commands.
  - **Why**: User-supplied requiredValue (version, env var name, dependency name) was interpolated into generated scripts; shell metacharacters could enable command injection.
  - **How**: Require `requiredValue` to match `^[a-zA-Z0-9._-]+$` before calling `generatePreview`.
- **File**: `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`
  - **Change**: Extended `validatePreRunScript` to validate requiredValue format for template-based types.
  - **Why**: Fail fast at save time with clear user-facing error.
  - **How**: Same regex; message: "Required value can only contain letters, numbers, dots, underscores, and hyphens."
- **File**: `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/EnvironmentTemplateExecutionTest.kt`
  - **Change**: Added test `should reject template parameter with shell metacharacters`.
  - **Why**: Regression protection for the security fix.
  - **How**: Assert that requiredValue "docker; rm -rf /" fails with "alphanumeric" in the error message.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions; EnvironmentCheckTemplate is a data class with clear structure.
- Project Structure: ✓ Registry in domain model; execution in infrastructure.
- Testing Strategy: ✓ Unit tests for registry and execution; validation test added.
- All ACs Met: ✓ AC1–AC5 covered.

### Improvements Checklist

- [x] Refactored template parameter validation (command-injection prevention)
- [x] Added regression test for invalid parameter
- [ ] Consider Windows-specific execution tests (optional)

### Security Review

- **Template parameter injection**: Fixed. requiredValue validated against allowlist before script generation.
- **Actionable messages**: No sensitive data in failure output; clear guidance for user.

### Performance Considerations

Template resolution and validation are in-memory. No concerns.

### Files Modified During Review

- `taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/prerun/LocalPreRunScriptService.kt`
- `taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt`
- `taskmanager/core/src/test/kotlin/com/aitask/core/infrastructure/prerun/EnvironmentTemplateExecutionTest.kt`

### Gate Status

Gate: PASS → docs/qa/gates/5.3-environment-check-templates.yml

### Recommended Status

✓ Ready for Done
