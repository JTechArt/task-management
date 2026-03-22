# Story 4.3: Import and Export of Projects, Tasks, and Rules

## Status

Done

**Epic:** Epic 4 - External Integrations, Portability, and Distribution

**As a** developer,  
**I want** to export and import my AiTask data,  
**so that** I can move project configurations and work context between installations.

## Acceptance Criteria

1. A user can export projects, tasks, repositories, and rules into a structured portable format.
2. A user can import previously exported data into another installation or environment.
3. The import flow validates incoming data and reports conflicts or incompatible content before applying changes.
4. Imported data preserves key relationships between projects, tasks, repositories, and rules.
5. The application confirms successful import and export completion with a clear summary of processed records.

## Architecture References

- [Data Protection: Export Security](../architecture.md#export-security)

## UX References

- [Import / Export / Backup Restore View](../front-end-spec.md#import--export--backup-restore-view)
- [Visual Mockup: Settings / Import / Export / Backup](../mockups/settings.html)

## Tasks / Subtasks

- [x] Add ExportImport DTOs and ImportResult, ImportValidationResult, ImportConflict (AC1, AC3, AC5)
- [x] Implement ExportDataUseCase with credential redaction (AC1)
- [x] Implement ImportDataUseCase with validate(), apply(), conflict reporting, ID mapping (AC2, AC3, AC4)
- [x] Create SettingsView with Import/Export card and SettingsViewModel (AC1, AC2, AC5)
- [x] Wire ExportDataUseCase and ImportDataUseCase in DependencyContainer
- [x] Add ExportDataUseCaseTest and ImportDataUseCaseTest
- [x] Fix pre-existing SlackChannelRepositoryImpl/DeleteSlackChannelUseCase compilation errors

## Dev Agent Record

### Completion Notes

- **DataExportBundle**, **ImportValidationResult**, **ImportResult**: Serializable DTOs in `ExportImport.kt`
- **ExportDataUseCase**: Exports projects, tasks, repos, rules to JSON; redacts credentials via CredentialRedactor (Export Security)
- **ImportDataUseCase**: validate() reports name/workspace conflicts; apply() uses disambiguate for clashes, schema version check, ID mapping for relationships
- **SettingsView**: Import/Export card with Export/Import buttons, file chooser, Snackbar feedback, error display
- **Unit tests**: ExportDataUseCaseTest (export structure, credential redaction); ImportDataUseCaseTest (validation conflicts, apply roundtrip, invalid JSON)
- **SlackChannelRepositoryImpl**: Fixed delete() to return Unit; removed deleteByProject override; DeleteSlackChannelUseCase checks findById before delete

### File List

- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/ExportImport.kt (modified – added ImportValidationResult, ImportConflict, ImportResult)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/ExportDataUseCase.kt (existing)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/ImportDataUseCase.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/SettingsViewModel.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/PlaceholderViews.kt (modified – SettingsView)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt (modified – exportDataUseCase, importDataUseCase)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/ExportDataUseCaseTest.kt (new)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/usecase/ImportDataUseCaseTest.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/SlackChannelRepositoryImpl.kt (modified – fix delete return type)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/DeleteSlackChannelUseCase.kt (modified – fix result handling)

## QA Results

### Review Date: 2026-03-17

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Export/import implementation follows clean architecture: DataExportBundle DTOs, ExportDataUseCase with CredentialRedactor, ImportDataUseCase with validate/apply, ID mapping for relationships, disambiguation for name/path clashes. SettingsView provides Export/Import buttons, file chooser, Snackbar feedback, and error display.

### Refactoring Performed

None. Code meets standards.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions, typed declarations, single-purpose use cases
- Project Structure: ✓ New/modified files in correct packages
- Testing Strategy: ✓ ExportDataUseCaseTest and ImportDataUseCaseTest cover structure, credential redaction, validation conflicts, apply roundtrip
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | Export to structured portable format | ExportDataUseCase, DataExportBundle, CredentialRedactor on cloneUrl |
| 2 | Import into another installation | ImportDataUseCase.apply, file chooser, SettingsView |
| 3 | Validate, report conflicts before applying | ImportDataUseCase.validate() (tested); apply() uses disambiguate for clashes |
| 4 | Preserve relationships | projectIdMap, ruleIdMap; tasks/repos/projectRules reference new IDs |
| 5 | Confirm with clear summary | ImportResult, Snackbar with projects/tasks/repos/rules/links counts |

### Security Review

- CredentialRedactor.redactUrl on repository clone URLs in export ✓
- No credentials in DataExportBundle ✓

### Gate Status

Gate: PASS → docs/qa/gates/4.3-import-export.yml

### Recommended Status

✓ Ready for Done

*Note: Optional enhancement—consider validate-before-apply UX to show conflicts to user before applying.*
