# TasK Manager Frontend Architecture Document

## Template and Framework Selection
- UI toolkit: JetBrains Compose Desktop (org.jetbrains.compose) with Material 3-inspired theming.
- Rationale: Pure JVM desktop stack (no Android artifacts), strong Kotlin-first APIs, multiplatform-ready for future extensions.
- Starter: Greenfield; Maven multi-module alongside backend. Manual setup for build, packaging, and CI/CD.

## Change Log
| Date | Version | Description | Author |
| --- | --- | --- | --- |
| 2025-12-14 | 0.1 | Initial frontend architecture draft aligned to backend architecture | Assistant |

## Frontend Tech Stack
| Category | Technology | Version | Purpose | Rationale |
| --- | --- | --- | --- | --- |
| Framework | JetBrains Compose Desktop | 1.7.x | Declarative UI for desktop | Kotlin-first, no Android deps |
| UI Library | Material 3-inspired components | - | Baseline theming, typography, surfaces | Consistent UX without Android artifacts |
| State Management | Kotlin coroutines + StateFlow | Kotlin 2.0.x | Reactive UI state | Structured concurrency, predictable state |
| Navigation | Sealed-route navigator (in-app) | - | Screen transitions, back stack | Simple, testable, desktop-friendly |
| DI | Koin | 3.x | Dependency injection | Lightweight, Kotlin DSL |
| Networking | Ktor Client (CIO) + kotlinx.serialization | 3.x / 1.7.x | HTTP to backend | Coroutine-native, multiplatform-ready |
| Data Persistence | SQLDelight (SQLite driver) | 2.x | Local cache, offline-friendly reads | Type-safe queries, pure JVM |
| Logging | Kotlin Logging + SLF4J backend | 5.x | Structured logs | Consistent with backend logging approach |
| Testing | JUnit 5, kotest assertions, turbine | 5.x / 1.x | Unit & state-flow testing | Clear Arrange-Act-Assert |
| UI Testing | Compose Desktop test APIs (org.jetbrains.compose.ui:ui-test-junit4) | 1.7.x | Component rendering/input tests | Desktop runner, avoid Android libs |
| Build Tool | Maven (multi-module) | 3.9+ | Build + dependency management | Aligns with backend |
| Packaging | jpackage | JDK 21 | Native installers (DMG/PKG, MSI/EXE, AppImage) | Cross-platform distribution |

## Project Structure (frontend modules)
```
task-management/
├── ui/                         # Compose Desktop module
│   ├── src/main/kotlin/
│   │   ├── app/                # Application entry + DI bootstrap
│   │   ├── navigation/         # Routes, navigator, nav host
│   │   ├── design/             # Theme, typography, spacing, icons
│   │   ├── feature/            # Feature packages (per bounded context)
│   │   │   ├── tasks/          # Screens, viewmodels, mappers
│   │   │   ├── projects/
│   │   │   └── rules/
│   │   ├── components/         # Reusable UI components
│   │   ├── state/              # Shared UI state contracts
│   │   ├── services/           # API client abstractions
│   │   ├── cache/              # SQLDelight DB + DAOs
│   │   ├── utils/              # Pure utilities (formatting, time)
│   │   └── platform/           # OS integrations (system tray, file pickers)
│   ├── src/test/kotlin/        # Unit tests
│   └── src/uiTest/kotlin/      # Compose UI tests
└── shared/                     # Shared models with backend (if needed)
    └── src/main/kotlin/
```

## Component Standards
### Component Template (Compose Desktop, JVM-only)
```kotlin
package app.feature.tasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

@Composable
fun TaskListScreen(
  state: TaskListState,
  onIntent: (TaskListIntent) -> Unit,
  modifier: Modifier = Modifier
) {
  TaskListScaffold(
    title = stringResource(Res.string.task_list_title),
    isLoading = state.isLoading,
    onRefresh = { onIntent(TaskListIntent.Refresh) },
    modifier = modifier
  ) {
    TaskList(
      tasks = state.items,
      onSelect = { id -> onIntent(TaskListIntent.OpenDetails(id)) }
    )
  }
}
```
Guidelines:
- Pure JVM imports only; no AndroidX artifacts beyond Compose Desktop-provided bundles.
- Separate state (`TaskListState`), intents (`TaskListIntent`), and UI.
- Keep components small; delegate to child composables for sections.

### Naming Conventions
- Files: `PascalCase.kt` per component or view model.
- Composables: `PascalCase`; events/intents: `PascalCase` sealed classes.
- ViewModels: `FeatureViewModel`.
- Routes: sealed interface `AppRoute`; destinations in `navigation` package.
- Resources: string keys `task_list_title`, icons in `design`.

## State Management
- Pattern: MVVM with unidirectional data flow.
- Data flow: UI -> Intent -> ViewModel -> UseCase/Repository -> StateFlow -> UI.
- Concurrency: structured coroutines (`viewModelScope` equivalent via custom scope).
- Error handling: Result/Outcome sealed types; surface user-friendly messages.

State holder example:
```kotlin
data class TaskListState(
  val isLoading: Boolean,
  val items: List<TaskSummary>,
  val errorMessage: String?
)

sealed interface TaskListIntent {
  data object Refresh : TaskListIntent
  data class OpenDetails(val taskId: String) : TaskListIntent
}
```

## API Integration
- HTTP client: Ktor CIO with JSON (kotlinx.serialization).
- Auth: bearer token stored in encrypted local storage; interceptor injects `Authorization`.
- Error policy: map HTTP errors to domain errors; retry with backoff for idempotent GETs.

Service template:
```kotlin
interface TaskService {
  suspend fun fetchTasks(): Result<List<TaskSummary>>
  suspend fun fetchTask(taskId: String): Result<TaskDetail>
}
```

Ktor client config (JVM-only):
```kotlin
fun buildHttpClient(config: HttpConfig): HttpClient =
  HttpClient(CIO) {
    install(ContentNegotiation) { json(config.json) }
    install(HttpTimeout) {
      requestTimeoutMillis = config.requestTimeoutMillis
      connectTimeoutMillis = config.connectTimeoutMillis
    }
    defaultRequest {
      url(config.baseUrl)
      header(HttpHeaders.Accept, ContentType.Application.Json)
      config.authTokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
  }
```

## Routing
- Sealed routes with arguments; simple stack navigator per window.
- Deep links not required; support back-stack for dialogs.

Example:
```kotlin
sealed interface AppRoute {
  data object Dashboard : AppRoute
  data class TaskDetail(val taskId: String) : AppRoute
}
```

## Styling Guidelines
- Material 3-inspired tokens implemented in `design` package (colors, typography, spacing).
- Theme object supplies light/dark palettes; typography uses JetBrains Mono/Inter equivalents.
- Avoid Android-specific theming APIs; use Compose Desktop `MaterialTheme`.

## Testing Requirements
- Unit: JUnit5 + kotest assertions for view models, mappers, repositories.
- State: turbine for Flow/StateFlow emissions.
- UI: Compose Desktop ui-test-junit4 for key screens; robot pattern for readability.
- Coverage: ≥80% lines/branches for UI module.

UI test sketch:
```kotlin
class TaskListScreenTest {
  @Test
  fun showsTasks() = runComposeUiTest {
    setContent {
      TaskListScreen(
        state = TaskListState(isLoading = false, items = sampleTasks(), errorMessage = null),
        onIntent = {}
      )
    }
    onNodeWithText("My Tasks").assertIsDisplayed()
  }
}
```

## Environment Configuration
- `UI_API_BASE_URL`: Backend base URL.
- `UI_API_TIMEOUT_MS`: Request timeout.
- `UI_LOG_LEVEL`: debug/info/warn/error.
- `UI_CACHE_PATH`: Path for SQLDelight database.
- `UI_TLS_TRUST_STORE` (optional): Custom trust store path for corporate proxies.

## Developer Standards (Frontend)
- No Android libraries; use only JVM + Compose Desktop artifacts.
- Keep composables side-effect free; side effects via `LaunchedEffect`/`DerivedState`.
- All network calls through service layer; no HTTP in UI.
- Shared models in `shared/` module when backend alignment is required.
- Respect backend contracts from `docs/backend-architecture.md` (PostgreSQL, Exposed, Ktor).
- Performance: avoid blocking UI dispatcher; prefer pagination and lazy lists.
- Accessibility: keyboard navigation for all focusable elements; contrast-safe colors; tooltips for icon-only actions.

## Packaging and Distribution
- Build: Maven + JDK 21, module `ui`.
- Packaging: jpackage outputs DMG/PKG (macOS), MSI/EXE (Windows), AppImage (Linux).
- Signing: macOS notarization and Windows code signing planned; placeholders in build profile.
- Updates: hook for auto-update (e.g., Sparkle/WinSparkle) future; not enabled in R1.

## Performance and Observability
- Targets: <3s cold start to first render; <200ms interaction latency for list actions.
- Logging: Kotlin Logging -> SLF4J backend; write to rotating file per OS conventions.
- Metrics: lightweight in-process counters; consider OpenTelemetry exporter later.

## Alignment with Backend Architecture
- Backend uses PostgreSQL 16 via Exposed/Hikari, Ktor client for Slack/JGit adapters; UI will consume the same backend API surface.
- Domain models mirrored in shared module where feasible to prevent drift.
- Authentication and error format align to backend spec once defined; wrap responses into Result types for UI consumption.

