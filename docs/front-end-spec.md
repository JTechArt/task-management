# TasK Manager UI/UX Specification

## Introduction
This document defines the user experience goals, information architecture, user flows, and visual design specifications for TasK Manager’s user interface. It serves as the foundation for visual design and frontend development, ensuring a cohesive and user-centered experience. It aligns with the JetBrains Compose Desktop stack, hexagonal modular architecture, and R1 scope (task list, detail/edit dialog, dashboard, filters) with WCAG 2.1 AA, ≤3s load, and ≤200ms interactions as quality gates.

## Overall UX Goals & Principles
### Target User Personas
- Power users managing many tasks and projects who need efficiency and shortcuts.
- Team leads who need oversight of status/type counts and recent activity.
- Individual contributors who update tasks and need clear validation.
- Administrators who manage rules and integrations (Git/IDE/Slack stubs).

### Usability Goals
- Fast comprehension: new users complete a core task action in <5 minutes.
- Efficiency: minimize clicks for frequent edits; preserve filters and context.
- Error prevention: inline validation and confirmation for destructive actions.
- Memorability: consistent patterns so infrequent users can return without relearning.
- Accessibility: WCAG 2.1 AA, keyboard-first navigation with visible focus.

### Design Principles
1. Clarity over cleverness.
2. Progressive disclosure for filters and detail complexity.
3. Consistent patterns across list, dialog, and dashboard.
4. Immediate feedback (toasts, inline errors, loading/error states).
5. Accessible by default (labels, focus rings, contrast).

### Change Log
| Date | Version | Description | Author |
| --- | --- | --- | --- |
| 2025-12-14 | 0.1 | Initial UI/UX specification draft | Assistant |

## Information Architecture (IA)
### Site Map / Screen Inventory
```mermaid
graph TD
  A[App Shell] --> B[Dashboard]
  A --> C[Tasks]
  A --> D[Projects]
  A --> E[Rules]
  A --> F[Settings]
  B --> B1[Status/Type Cards]
  B --> B2[Recent Activity]
  C --> C1[Task List]
  C --> C2[Task Detail/Dialog]
  C --> C3[Filters]
  D --> D1[Project List]
  D --> D2[Project Detail]
  E --> E1[Rules List]
  E --> E2[Rule Detail]
  F --> F1[Preferences]
  F --> F2[Integrations (Git/IDE/Slack stubs)]
```

### Navigation Structure
**Primary Navigation:** Dashboard, Tasks, Projects, Rules, Settings.  
**Secondary Navigation:** Contextual tabs/actions within a section (e.g., task filters panel, project detail tabs).  
**Breadcrumb Strategy:** Single-level within detail contexts (Tasks › Task Detail); dialog-driven flows keep back-stack shallow.

## User Flows
### Flow: Task List → Detail/Edit (refined)
**User Goal:** Review tasks, filter, and edit details quickly.  
**Entry Points:** App shell Tasks; deep link from dashboard cards; post-create return.  
**Success Criteria:** Task updated/viewed; filters preserved; feedback shown; list/scroll/selection state retained.

```mermaid
graph TD
  A[Open Tasks] --> A1[Load Data]
  A1 -->|Success| B[Filters Adjusted]
  A1 -->|Error| A2[Show Error + Retry]
  A1 -->|Empty| A3[Empty State + Reset Filters]
  B --> C[Task List Updates (debounced)]
  C --> D[Select Task]
  D --> E[Open Detail/Edit Dialog (focus trap)]
  E --> F{Unsaved Changes?}
  F -->|Yes + Esc| F1[Confirm Close or Stay]
  F -->|No| G{Edit?}
  G -->|Yes| H[Validate + Save (pending state, shortcuts: Cmd/Ctrl+S)]
  G -->|No| L[Close Dialog]
  H --> I{Save Success?}
  I -->|Yes| J[Success Toast + Refresh + Highlight Item]
  I -->|No| K[Inline Errors + Retry]
  J --> M[Return to List with Filters/Selection/Scroll Preserved]
  L --> M
  K --> E
```

**Edge Cases & Handling**
- Preserve filters, selection, and scroll when returning.
- Empty results: show empty state with “Reset filters”.
- Save failures: keep dialog data, inline errors, retry; avoid data loss.
- Status transitions: validate rules before save; surface inline guidance.
- Keyboard: tab order through filters → list → dialog; Esc with unsaved guard; Enter submits when valid; shortcut to focus filters (Cmd/Ctrl+F) and save (Cmd/Ctrl+S).
- Performance: debounce filters; target <500ms refresh @1k tasks; keep network off UI thread; highlight updated item after refresh; handle stale data with non-blocking notice and refresh.

### Flow: Task Creation with Workspace/Branch Generation
**User Goal:** Create a task that persists with generated workspacePath and branchName.  
**Entry Points:** Tasks list “New Task” button; shortcut Cmd/Ctrl+N; optional from project detail.  
**Success Criteria:** Task created with deterministic workspacePath `{base}/{sanitizedProjectName}/{taskId-short}` and branchName per project template; user sees confirmation and can open the task. Tasks cannot exist without a project—enforce selection/creation first.

```mermaid
graph TD
  A[Click New Task] --> A0{Projects exist?}
  A0 -->|No| A01[Prompt: Create Project First] --> A02[Open Project Create]
  A0 -->|Yes| B[Open Create Dialog (focus first field)]
  B --> C[Select Project (required) + Fill Fields]
  C --> D[Validate Inputs]
  D -->|Invalid| D1[Inline Errors + Stay]
  D -->|Valid| E[Generate workspacePath + branchName]
  E --> F[Save Task (pending state)]
  F --> G{Save Success?}
  G -->|Yes| H[Success Toast + Add to List + Highlight]
  G -->|No| I[Show Error + Keep Dialog Data + Retry]
  H --> J[Close Dialog or Open Detail]
  I --> B
```

**Edge Cases & Handling**
- Project requirement: tasks cannot exist without a project. If none exists, block task creation and route to project creation; prefill return path.
- Validation: required fields (including project), status/type enums, branch template substitution errors; inline errors, no data loss.
- Determinism: sanitization rules for project name; short taskId; default template `task-{taskId}` if missing.
- Keyboard: tab order; Esc with unsaved guard; Enter submits when valid.
- Performance: keep generation and save off UI thread; avoid blocking UI.

### Flow: Dashboard Aggregates Refresh and Error Handling
**User Goal:** View status/type counts and recent activity; refresh data and recover from errors.  
**Entry Points:** Dashboard route on app open or nav click; manual refresh button/shortcut.  
**Success Criteria:** Aggregates load quickly; errors are recoverable; cards/recents reflect latest data.

```mermaid
graph TD
  A[Open Dashboard] --> B[Load Aggregates + Recent Activity]
  B -->|Success| C[Render Cards + Recent List]
  B -->|Error| D[Show Error State + Retry CTA]
  B -->|Empty| E[Show Empty State + Guidance]
  C --> F[Manual Refresh (button/shortcut)]
  F --> B
  D --> F
```

**Edge Cases & Handling**
- Loading: skeleton states for cards/list; keep layout stable.
- Errors: non-blocking toast plus inline error on cards; retry CTA; preserve last-known-good if available.
- Empty: guidance text and “Create task”/“Create project” affordance.
- Performance: target <3s initial load; <200ms interaction; debounce manual refresh; avoid blocking UI dispatcher.
- Accessibility: focus returns to top card on refresh; keyboard access to retry and refresh; semantics for cards and list.

### Flow: Project List and Create/Edit
**User Goal:** View projects, create or edit with validation, and delete with cascade warning.  
**Entry Points:** Projects nav; “Create Project” button; edit from list row.  
**Success Criteria:** Project created/updated with unique name, valid repositoryUrl, and branchTemplate; delete confirms cascade to tasks.

```mermaid
graph TD
  A[Open Projects] --> B[Load Projects]
  B -->|Success| C[List Projects]
  B -->|Error| B1[Error + Retry]
  B -->|Empty| B2[Empty State + Create CTA]
  C --> D[Create Project]
  C --> E[Edit Project]
  C --> F[Delete Project]
  D --> G[Fill Form (name, repositoryUrl, branchTemplate)]
  E --> G
  G --> H[Validate (unique name, URL regex)]
  H -->|Invalid| H1[Inline Errors + Stay]
  H -->|Valid| I[Save (pending)]
  I --> J{Save Success?}
  J -->|Yes| K[Toast + Refresh List + Highlight]
  J -->|No| L[Show Error + Keep Data + Retry]
  F --> M[Confirm Delete with Cascade Warning]
  M -->|Confirm| N[Delete + Toast + Refresh]
  M -->|Cancel| C
```

**Edge Cases & Handling**
- Unique name: inline error if duplicate; disable submit until resolved.
- repositoryUrl: regex validation with inline error; allow save only when valid.
- branchTemplate: show default (e.g., `task-{taskId}`); validate substitution tokens.
- Delete: explicit modal warning that tasks will be deleted via cascade; require confirm action.
- Error states: non-blocking retry; preserve form data on failure.
- Accessibility: focus trap in dialogs; Esc guard on unsaved changes; Enter submits when valid; tab order across fields; clear focus ring.

## Wireframes & Mockups
- Deliver static mockups and an interactive HTML prototype covering task list + filters, task detail/edit dialog (create/edit, validation, status transitions), dashboard cards (loading/empty/error/success), and settings (integrations stub).
- States: loading, empty, error, success for lists and dashboard; success/error for dialog actions.
- Accessibility notes: focus order, keyboard shortcuts (Cmd/Ctrl+F filters, Cmd/Ctrl+S save), clear focus ring, high contrast, tooltips on icon-only controls.
- Responsive/adaptive: desktop-first; narrow view collapses sidebar and presents filters as drawer.
- Files to store under `doc/tasks/mockups/` with descriptive names.

## Component Library / Design System
- Approach: Lightweight Compose-ready token set (colors, typography, spacing) inspired by Material 3; no Android dependencies; grow as components solidify.
- Core Components: AppShell (nav + top bar), Toolbar, Filters Panel (collapsible/drawer), Data List with row states, Dialog with form fields and inline validation, Toast/Inline Status, Dashboard Cards (loading/skeleton/error/success).
- States baked into components (loading/empty/error/success) to keep consistency and reduce duplication.

## Branding & Style Guide
### Visual Identity
Brand guidelines: link TBD (provide brand source to refine).

### Color Palette
| Color Type | Hex Code | Usage |
| --- | --- | --- |
| Primary | #2F5FFF | Primary actions, focus |
| Secondary | #4B5565 | Secondary text/icons |
| Accent | #F59E0B | Highlights |
| Success | #16A34A | Positive feedback |
| Warning | #D97706 | Cautions |
| Error | #DC2626 | Errors, destructive actions |
| Neutral | #0F172A / #1F2937 / #E5E7EB | Text, borders, backgrounds |

### Typography
- Primary: Inter  
- Secondary: Inter  
- Monospace: JetBrains Mono  
- Type scale (to tune in mockups):
| Element | Size | Weight | Line Height |
| --- | --- | --- | --- |
| H1 | TBD | TBD | TBD |
| H2 | TBD | TBD | TBD |
| H3 | TBD | TBD | TBD |
| Body | TBD | TBD | TBD |
| Small | TBD | TBD | TBD |

### Iconography
- Icon Library: Feather/Phosphor-style vectors.  
- Usage Guidelines: Consistent stroke weight, tooltips on icon-only buttons, clear labels for assistive tech.

### Spacing & Layout
- Grid System: 4/8 base grid.  
- Spacing Scale: 4, 8, 12, 16, 20, 24, 32; comfortable spacing in dialogs and lists.

## Accessibility Requirements
- Compliance Target: WCAG 2.1 AA; keyboard-first support.
- Visual: Contrast per AA; clear focus indicators; scalable text with sensible minimum sizes.
- Interaction: Full keyboard navigation; screen reader labels/roles; shortcuts for filters (Cmd/Ctrl+F) and save (Cmd/Ctrl+S); touch targets ≥44px if touch is used.
- Content: Alt text for icons/images; semantic headings; explicit form labels and inline errors.
- Testing Strategy: Automated lint + manual checks (focus order, screen reader pass, high-contrast theme check) on key flows (task list/filter, detail/edit dialog, dashboard cards).

## Responsiveness Strategy
- Breakpoints: Mobile 360–767px; Tablet 768–1023px; Desktop 1024–1439px; Wide 1440px+.  
- Adaptation Patterns: Sidebar collapses to rail/drawer on narrow; filters become slide-in panel; cards wrap. Primary nav stays; secondary actions move to overflow on narrow. Content priority favors task list and primary actions; secondary metadata becomes expandable. Maintain keyboard shortcuts and focus order in each layout.

## Animation & Micro-interactions
- Motion Principles: Subtle and purposeful; respect prefers-reduced-motion; use FastOutSlowIn-style easing; durations 150–250ms.
- Key Animations:
  - Filter drawer open/close: 180ms ease-out; focus moves to first filter.
  - Dialog open/close: 200ms fade/scale; initial focus on first field; Esc close guard.
  - List item highlight on update: ~600ms background fade.
  - Toast slide/fade: ~180ms; auto-dismiss with manual close.
  - Skeleton/loading shimmer: low-contrast, ~1200ms, disabled when reduced-motion is set.

## Performance Considerations
- Goals: ≤3s app init to first render; ≤200ms interaction for filters/dialog; ≤500ms list refresh @1k tasks; 60fps animation respecting reduced motion.
- Strategies: Debounce filters; lazy lists; skeleton states; avoid blocking UI dispatcher; keep network on background coroutines; optimize assets; cache where appropriate.

## Next Steps
### Immediate Actions
1. Review and approve mockup plan; generate static + interactive prototypes for task list, detail/edit dialog, and dashboard states.
2. Capture final color/type scale and icon set decisions (replace TBD values).
3. Align accessibility checklist with engineering tests (focus, keyboard, reduced motion).
4. Lock responsive/adaptive behaviors (sidebar collapse, filter drawer).

### Design Handoff Checklist
- All user flows documented
- Component inventory complete
- Accessibility requirements defined
- Responsive strategy clear
- Brand guidelines incorporated
- Performance goals established

## Checklist Results
UI/UX checklist not yet run; execute after mockups and spec approval.

