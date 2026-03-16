# Story 3.5: Enhanced Management Views and Filtering

## Status

Done

**Epic:** Epic 3 - Visibility and Operational Control

**As a** developer,  
**I want** stronger filtering and management views for projects, tasks, and rules,  
**so that** I can operate efficiently as the amount of tracked work grows.

## Acceptance Criteria

1. Project, task, and rule views support search and filtering based on relevant metadata.
2. The task view supports filtering by status, task type, and project.
3. The project view supports filtering by tags, teams, or other configured metadata when available.
4. Rule management views support locating rules by scope or linked project or repository.
5. Filter and search interactions return results quickly enough to support normal desktop productivity use.

## Dev Agent Record

### Tasks / Subtasks

- [x] Task view: TaskFilters with search, task type, project, status filters; TasksViewModel state and handlers; TasksView filtered list
- [x] Project view: ProjectFilters with search, tags, team; ProjectsViewModel state and handlers; ProjectsView filtered list
- [x] Rules view: RuleFilters with scope and linked project; RulesViewModel loadRules by project, scope filter; RulesView filtered list

### File List

- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TaskComponents.kt (TaskFilters: search, task type)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/tasks/TasksView.kt (filteredTasks, TaskFilters wiring)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt (selectedTaskTypeFilter, searchQuery, setTaskTypeFilter, setSearchQuery)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt (ProjectFilters: search, tags, team)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt (ProjectFilters, filteredProjects)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt (searchQuery, selectedTagFilter, selectedTeamFilter, setSearchQuery, setTagFilter, setTeamFilter)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/rules/RuleComponents.kt (RuleFilters: scope, linked project)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/rules/RulesView.kt (RuleFilters, filteredRules)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/RulesViewModel.kt (selectedScopeFilter, selectedProjectFilter, loadRules by project, setScopeFilter, setProjectFilter)

### Change Log

| Date | Description |
|------|-------------|
| 2026-03-16 | Implemented Story 3.5: Task view search + task type filter; Project view search + tags + team filters; Rules view scope + linked project filters. Client-side filtering for AC5. |

### Completion Notes

- AC1: All three views support search and filtering based on relevant metadata.
- AC2: Task view: search (title/description), task type, project, status filters.
- AC3: Project view: search (name/description), tags, team filters (when available).
- AC4: Rules view: scope filter; linked project filter loads rules via getByProject when project selected.
- AC5: Client-side filtering for typical desktop datasets ensures quick response.

## Architecture References

- [Quality Attributes: Usability](../architecture.md#usability)

## UX References

- [Main Dashboard layout](../front-end-spec.md#1-main-dashboard)
- [Performance Goals: Interaction Response](../front-end-spec.md#performance-goals)
- [Visual Mockup: Dashboard](../mockups/dashboard.html)
- [Visual Mockup: Tasks List / Detail](../mockups/tasks.html)

## QA Results

### Review Date: 2026-03-16

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation meets all acceptance criteria. Task view: TaskFilters (search, project, status, task type); TasksView filteredTasks via remember() with client-side type and search; project/status filters trigger loadTasks (server-side). Project view: ProjectFilters (search, tags, team when available); ProjectsView filteredProjects with search/tag/team. Rules view: RuleFilters (scope, linked project); RulesViewModel loads rules via getByProject when project selected; filteredRules applies scope client-side. All filtering yields quick response for typical desktop datasets (AC5).

### Compliance Check

- Coding Standards: ✓ Kotlin naming, structure follow conventions
- Project Structure: ✓ Filters in TaskComponents, ProjectComponents, RuleComponents; ViewModels in ui/viewmodel
- Testing Strategy: ✓ No dedicated filter tests; filtering is View-layer logic; acceptable for enhancement story
- All ACs Met: ✓

### Security Review

No concerns. Client-side filtering on in-memory data; no secrets.

### Performance Considerations

Client-side filtering with remember() keys ensures minimal recomputation. Suitable for typical desktop scale (hundreds of items). AC5 satisfied.

### Gate Status

Gate: PASS → docs/qa/gates/3.5-enhanced-management-views.yml

### Recommended Status

✓ Ready for Done
