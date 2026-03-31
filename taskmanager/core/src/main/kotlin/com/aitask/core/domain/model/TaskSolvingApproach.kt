package com.aitask.core.domain.model

import java.util.UUID

/**
 * User-reviewable solving plan produced by [com.aitask.core.domain.usecase.GenerateTaskApproachUseCase].
 */
data class TaskSolvingApproach(
    val summary: String,
    val steps: List<TaskApproachStep>,
    val contextAgentId: UUID?,
    val contextAgentName: String?
)

data class TaskApproachStep(
    val title: String,
    val detail: String,
    val suggestedTools: List<AgentToolKind>,
    val prompt: String?
)

enum class TaskApproachReviewState {
    NONE,
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
