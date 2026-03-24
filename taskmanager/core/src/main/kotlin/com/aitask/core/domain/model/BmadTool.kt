package com.aitask.core.domain.model

enum class BmadToolType {
    AGENT,
    TASK,
    CHECKLIST
}

data class BmadTool(
    val id: String,
    val label: String,
    val type: BmadToolType,
    val description: String
)

object BmadToolCatalog {
    val tools: List<BmadTool> = listOf(
        BmadTool("agent:ux-expert", "UX Expert", BmadToolType.AGENT, "UI and front-end specification support"),
        BmadTool("agent:sm", "Scrum Master", BmadToolType.AGENT, "Story drafting and delivery workflow support"),
        BmadTool("agent:qa", "Quality Advisor", BmadToolType.AGENT, "Test strategy, traceability, and gate reviews"),
        BmadTool("agent:po", "Product Owner", BmadToolType.AGENT, "Backlog and acceptance criteria management"),
        BmadTool("agent:pm", "Product Manager", BmadToolType.AGENT, "PRDs, epics, and product planning"),
        BmadTool("agent:dev", "Full Stack Developer", BmadToolType.AGENT, "Implementation and debugging support"),
        BmadTool("agent:bmad-orchestrator", "BMAD Orchestrator", BmadToolType.AGENT, "Workflow coordination across BMAD roles"),
        BmadTool("agent:bmad-master", "BMAD Master", BmadToolType.AGENT, "General-purpose BMAD execution"),
        BmadTool("agent:architect", "Architect", BmadToolType.AGENT, "Architecture and technical design guidance"),
        BmadTool("agent:analyst", "Business Analyst", BmadToolType.AGENT, "Discovery and analysis support"),
        BmadTool("task:create-next-story", "Create Next Story", BmadToolType.TASK, "Generate the next delivery story"),
        BmadTool("task:review-story", "Review Story", BmadToolType.TASK, "QA review workflow for stories"),
        BmadTool("task:correct-course", "Correct Course", BmadToolType.TASK, "Realign stories or plans when work drifts"),
        BmadTool("task:execute-checklist", "Execute Checklist", BmadToolType.TASK, "Run BMAD checklist workflows"),
        BmadTool("task:test-design", "Test Design", BmadToolType.TASK, "Create structured test scenarios"),
        BmadTool("task:trace-requirements", "Trace Requirements", BmadToolType.TASK, "Map requirements to tests"),
        BmadTool("checklist:story-dod-checklist", "Story DoD Checklist", BmadToolType.CHECKLIST, "Developer definition of done checks"),
        BmadTool("checklist:story-draft-checklist", "Story Draft Checklist", BmadToolType.CHECKLIST, "Scrum story quality checks"),
        BmadTool("checklist:po-master-checklist", "PO Master Checklist", BmadToolType.CHECKLIST, "Product owner artifact review"),
        BmadTool("checklist:pm-checklist", "PM Checklist", BmadToolType.CHECKLIST, "Product manager quality review"),
        BmadTool("checklist:architect-checklist", "Architect Checklist", BmadToolType.CHECKLIST, "Architecture review support")
    )

    val defaultToolIds: List<String> = tools.map { it.id }

    fun labelsFor(ids: List<String>): List<String> =
        ids.mapNotNull { id -> tools.find { it.id == id }?.label }
}
