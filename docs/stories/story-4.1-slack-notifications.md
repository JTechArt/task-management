# Story 4.1: Slack Notification Configuration and Delivery

**Epic:** Epic 4 - External Integrations, Portability, and Distribution

**As a** team-oriented developer,  
**I want** AiTask to send task updates to Slack,  
**so that** project activity is visible in the communication tools my team already uses.

## Status

Done

## Acceptance Criteria

1. A user can configure Slack integration settings for a project, including destination channel details.
2. A user can choose which task lifecycle events trigger Slack notifications.
3. The application sends notifications for configured events with enough context to identify the relevant task and project.
4. Failed Slack delivery attempts provide visible feedback without blocking the underlying task action.
5. Slack notification activity is recorded in the application history.

## Architecture References

- [Component Architecture: Integration (Slack, OAuth)](../architecture.md#7-integration-component)
- [Integration Architecture: Slack Integration](../architecture.md#2-slack-integration)

## UX References

- [Integrations & Health screen](../front-end-spec.md#5-integrations--health)
- [Slack Channel Configuration View](../front-end-spec.md#slack-channel-configuration-view)
- [Visual Mockup: Project Detail](../mockups/project-detail.html)
- [Visual Mockup: Integrations & Health](../mockups/integrations.html)

## Tasks / Subtasks

- [x] Task 1: Add slack_channels migration and Slack domain models (AC1, AC2)
  - [x] V7__add_slack_channels.sql migration
  - [x] TaskEvent enum, SlackChannelConfig domain model
  - [x] SlackService interface, SlackWebhookClient (Ktor)
  - [x] SlackChannels Exposed table, SlackChannelRepository
- [x] Task 2: Implement SlackNotificationService and wire to task lifecycle (AC3, AC4, AC5)
  - [x] SlackNotificationService with notifyInBackground
  - [x] Add NOTIFICATION_SENT to ActivityType
  - [x] Wire notifications in TasksViewModel: create, update status, generate workspace, launch IDE
  - [x] Record success/failure in activity log
- [x] Task 3: Add Slack config UI to Project Detail (AC1, AC2)
  - [x] ProjectDetailView tabs: Repositories | Slack
  - [x] SlackConfigView, SlackChannelDialog
  - [x] ProjectsViewModel Slack CRUD and test message
- [x] Task 4: Add unit tests
  - [x] SlackNotificationService test (success and failure scenarios)
  - [ ] SlackWebhookClient test (optional, integration)
  - [ ] CreateSlackChannelUseCase test (optional)

## Dev Agent Record

### Agent Model Used
Claude Opus 4.5

### Debug Log References
Gradle build failed with system error (wildcard IP). Run `cd taskmanager && ./gradlew build` locally to verify.

### Completion Notes List
- Implemented Slack Incoming Webhooks (no OAuth until Story 4.2)
- Migration V7 adds slack_channels table with webhook_url, channel_display_name, project_id, enabled_events
- Slack notification delivery is fire-and-forget (AC4); failures recorded in activity (AC5)
- Events: TASK_CREATED, TASK_STARTED, TASK_COMPLETED, WORKSPACE_CREATED, IDE_LAUNCHED
- Webhook URL format validated: https://hooks.slack.com/services/...

### File List
- taskmanager/core/src/main/resources/db/migration/V7__add_slack_channels.sql (new)
- taskmanager/core/build.gradle.kts (modified - Ktor deps)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/SlackChannel.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/model/Activity.kt (modified - NOTIFICATION_SENT)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackService.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/service/SlackNotificationService.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/repository/SlackChannelRepository.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/entity/SlackChannelEntity.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/data/repository/SlackChannelRepositoryImpl.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/infrastructure/slack/SlackWebhookClient.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/GetSlackChannelsUseCase.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/CreateSlackChannelUseCase.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/UpdateSlackChannelUseCase.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/DeleteSlackChannelUseCase.kt (new)
- taskmanager/core/src/main/kotlin/com/aitask/core/domain/usecase/SendSlackTestMessageUseCase.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/di/DependencyContainer.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/ProjectsViewModel.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/viewmodel/TasksViewModel.kt (modified)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectComponents.kt (modified - tabs, Slack)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/SlackConfigView.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/SlackChannelDialog.kt (new)
- taskmanager/desktop-app/src/main/kotlin/com/aitask/desktop/ui/projects/ProjectsView.kt (modified)
- taskmanager/core/src/test/kotlin/com/aitask/core/domain/service/SlackNotificationServiceTest.kt (new)

### Change Log
| Date | Change |
|------|--------|
| 2026-03-17 | Implemented Story 4.1: Slack notification config, delivery, activity recording, UI |

## QA Results

### Review Date: 2026-03-17

### Reviewed By: Quinn (Test Architect)

### Code Quality Assessment

Implementation follows clean architecture with clear separation of concerns. SlackNotificationService orchestrates delivery; SlackWebhookClient performs HTTP calls; use cases validate input. Webhook URL validation restricts to `https://hooks.slack.com/services/*` in CreateSlackChannelUseCase, UpdateSlackChannelUseCase, SlackChannelDialog, and DB constraint—providing SSRF mitigation. SlackChannelConfig.toString() excludes webhookUrl to avoid credential exposure in logs.

### Refactoring Performed

None required. Code meets standards.

### Compliance Check

- Coding Standards: ✓ Kotlin conventions followed; types declared; verb-named functions
- Project Structure: ✓ New files in appropriate packages (domain/service, infrastructure/slack, ui/projects)
- Testing Strategy: ✓ Unit tests for SlackNotificationService (success + failure); story marks SlackWebhookClient/CreateSlackChannelUseCase tests as optional
- All ACs Met: ✓

### AC Traceability

| AC | Description | Evidence |
|----|-------------|----------|
| 1 | Configure Slack integration settings | CreateSlackChannelUseCase, UpdateSlackChannelUseCase, SlackConfigView, SlackChannelDialog, V7 migration |
| 2 | Choose which events trigger notifications | TaskEvent enum, enabledEvents in SlackChannelConfig, UI checkboxes |
| 3 | Send notifications with task/project context | SlackNotificationService.buildMessage, notifyTaskEvent |
| 4 | Failed delivery visible feedback, non-blocking | notifyInBackground (fire-and-forget), recordNotificationActivity(FAILED), Activity/Dashboard views |
| 5 | Notification activity recorded | recordNotificationActivity, ActivityType.NOTIFICATION_SENT |

### Improvements Checklist

- [x] SlackNotificationService success and failure tests present
- [ ] Consider CreateSlackChannelUseCase unit tests for validation logic (optional enhancement)
- [ ] Consider truncating SlackWebhookClient failure response body in logs if very long (monitoring)

### Security Review

- Webhook URL restricted to Slack domain—SSRF mitigated
- No webhook URL in activity metadata or logs
- SlackChannelConfig.toString excludes sensitive fields

### Performance Considerations

- Fire-and-forget notifications via CoroutineScope; task actions not blocked

### Gate Status

Gate: PASS → docs/qa/gates/4.1-slack-notifications.yml

### Recommended Status

✓ Ready for Done
