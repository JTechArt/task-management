# TasK Manager Architecture Document

## Introduction
This document outlines the overall project architecture for TasK Manager, including backend systems, shared services, and non-UI specific concerns. Its primary goal is to serve as the guiding architectural blueprint for AI-driven development, ensuring consistency and adherence to chosen patterns and technologies.

**Relationship to Frontend Architecture:** If the project includes a significant user interface, a separate Frontend Architecture Document will detail the frontend-specific design and MUST be used in conjunction with this document. Core technology stack choices documented herein (see "Tech Stack") are definitive for the entire project, including any frontend components.

### Starter Template or Existing Project
- Starter template or existing codebase: not identified.
- Inputs reviewed: docs/prd.md. No repository or starter links provided.
- Decision: proceed as greenfield; expect manual setup for tooling. A Kotlin/JVM 21 + Compose Desktop starter (e.g., Maven multi-module) can be suggested later if desired.

### Change Log
| Date | Version | Description | Author |
| --- | --- | --- | --- |

## High Level Architecture

### Technical Summary
- Kotlin/JVM 21 Compose Desktop modular monolith using Hexagonal ports/adapters; modules for UI, application services, domain, infrastructure; adapters: Exposed/Hikari to PostgreSQL 16 (Docker), JGit for Git intents, Ktor client for Slack, IDE launcher stub.
- Maven multi-module build; Flyway migrations at startup; jpackage installers. Performance guardrails: lazy IO init, index-backed queries, pagination-ready list rendering.
- Integration points: JGit (repo intents now, full Git ops later), Slack via Ktor client, IDE launcher stub (Cursor now; JetBrains/Augment later).
- Local PostgreSQL 16 in Docker; app connects via HikariCP; configuration can point to remote DB later.
- Packaging: Maven fat JAR plus jpackage installers; optional Dockerized app+DB for reproducible environment.

### High Level Overview
1. Architectural style: Modular monolith (desktop app) with Hexagonal ports/adapters; single-process app calling local/localhost PostgreSQL container.
2. Repository structure: Monorepo (single Maven multi-module) to share domain/types across UI and integrations.
3. Service architecture: Single app process; internal modules for ui, domain, data, integrations, infra; no distributed services in R1.
4. Primary flow: UI → ViewModels → use cases → repositories (Exposed) → PostgreSQL; integration adapters (JGit/Slack/IDE launcher) via domain ports; responses mapped back to UI state.
5. Key decisions and rationale: Hexagonal to isolate UI and integrations, easing future IDE/Git expansions; monorepo keeps domain single source; PostgreSQL for ACID and performance; Dockerized DB for consistency; leave IPC/REST only if future plugins require it.

### High Level Project Diagram
```mermaid
graph TD
  UI[Compose Desktop UI] --> VM[ViewModels / Use Cases]
  VM --> Domain[Domain Layer]
  Domain --> Repo[Repositories (Ports)]
  Repo -->|Exposed/Hikari| DB[(PostgreSQL 16 in Docker)]
  Domain --> JGit[JGit Adapter]
  Domain --> Slack[Slack Adapter (Ktor client)]
  Domain --> IDE[IDE Launcher Stub]
  VM --> Metrics[Dashboard Aggregations]
```

### Architectural and Design Patterns
- Architectural style options: {Hexagonal, Layered, CQRS-lite}. **Recommendation: Hexagonal** to decouple UI and integrations, enabling swap-friendly adapters and future IDE/Git additions.
- Code organization options: {Clean Architecture rings, classic 3-layer, feature modules}. **Recommendation: Clean/Hex rings with feature submodules** mapped to task, project, rule contexts.
- Data patterns options: {Repository per aggregate, DAO, Event Sourcing (future)}. **Recommendation: Repository per aggregate** using Exposed; no event sourcing in R1.
- Communication options: {In-process calls, REST/HTTP local service, gRPC}. **Recommendation: In-process calls** (desktop monolith); expose HTTP only if future plugins need IPC.
- Integration resilience options: {Retry/backoff + circuit breakers, fire-and-forget}. **Recommendation: Retry/backoff with circuit breaker for Slack/Git when added, configurable}.

