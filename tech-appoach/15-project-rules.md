# Project Rules (Kotlin Desktop)

Guiding rules for implementing the Kotlin desktop application with Compose Desktop, Maven, and PostgreSQL. These are the defaults unless a feature spec documents an approved exception.

## Platform & Tooling
- Kotlin 2.0+, Java 21 as the JVM target; Compose Desktop 1.6+.
- Maven 3.9+ with the Kotlin, Compose, and JPackage plugins; keep reproducible builds.
- Target Compose Multiplatform for macOS/Windows/Linux; prefer native distributions via JPackage.

## Architecture & Modules
- Follow the layered structure in `14-architecture-overview.md` (ui → domain → data → infrastructure).
- Keep UI declarative and state-hoisted; domain pure and platform-agnostic; data layer isolated behind interfaces.
- Encapsulate side effects (I/O, DB, network) in use cases or services; prefer dependency inversion.

## Coding Standards
- Follow Kotlin coding conventions; enforce ktlint/detekt in CI and pre-commit.
- Use descriptive names; avoid abbreviations. Keep functions focused (<50 lines) and prefer small data classes/records.
- Prefer `val` and immutability; avoid shared mutable state. No `!!` unless justified; model nullability explicitly.
- Use sealed classes for UI/view-state/result types; prefer `Result`/either-style wrappers for operations that can fail.
- Extension functions only when they are cohesive with the extended type; avoid broad utils dumping ground.

## Concurrency & Coroutines
- Use structured concurrency; always launch coroutines in a scoped context (e.g., `viewModelScope`, `coroutineScope`).
- Keep coroutine dispatchers explicit; confine I/O to `Dispatchers.IO`, UI updates to `Dispatchers.Main`.
- Avoid global `CoroutineScope`; cancel jobs on lifecycle end (window/feature scope).

## Compose UI Rules
- Stateless composables by default; state hoisted to view models or controllers.
- Use `remember` only for UI state; persist business state in view models.
- Preview-friendly composables: provide default parameters and fake data builders.
- Keep recomposition cheap—avoid heavy work in composables; move logic to view models/use cases.

## Data & Database
- Use Exposed DSL/DAO consistently; no raw SQL in business code.
- All schema changes go through Flyway migrations; never modify migrations after merge—add a new one.
- Connection pooling via HikariCP; enforce prepared statements and parameter binding only.
- Prefer optimistic locking/version columns for concurrent updates where applicable.

## Error Handling & Logging
- Fail fast on programmer errors with `require`/`check`; use domain errors for recoverable cases.
- Never swallow exceptions; either handle with user feedback or propagate as typed results.
- Log with structured logging (SLF4J + chosen backend); no `println`.
- Map technical errors to user-friendly messages at the UI boundary.

## Configuration & Secrets
- No secrets in repo. Read secrets from environment or OS keychain; support `.env` for local only (git-ignored).
- Use typed configuration objects; validate on startup and fail fast if invalid.

## Testing
- Unit: JUnit 5 + MockK (or Kotest matchers) for fast tests; keep business logic pure and testable.
- UI: Compose UI tests for screen flows and accessibility checks.
- Integration: Test DB with Testcontainers PostgreSQL; migrations must run in tests.
- Require tests for new features/bug fixes; aim for meaningful coverage over percentage chasing.

## Git Workflow
- Branches: `feature/*`, `fix/*`, `chore/*`, `docs/*`. Rebase before merge; keep history linear on `main`.
- Commits: messages ≤64 chars (project rule); small, cohesive changes. No mixed concerns.
- Reviews: at least one approval; block merges on failing CI/lint/tests.

## Quality Gates
- Mandatory checks: ktlint, detekt, unit tests, integration tests (where applicable), dependency vulnerability scan.
- Static analysis must run in CI; treat warnings as build failures unless documented otherwise.

## Releases & Packaging
- Version with SemVer. Tag releases from `main`.
- Ship installers with JPackage; include migration step in startup for DB changes.

## Exceptions
- Document any rule exception in the relevant spec and link it from the PR description. Temporary exceptions require a cleanup issue.

