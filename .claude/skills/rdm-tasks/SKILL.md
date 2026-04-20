---
name: rdm-tasks
description: Work on rdm tasks
allowed-tools:
  - Read
  - Glob
  - Grep
  - Write
  - Edit
  - EnterPlanMode
  - ExitPlanMode
  - mcp__rdm__rdm_task_list
  - mcp__rdm__rdm_task_show
  - mcp__rdm__rdm_task_update
---

Work on rdm tasks. `$ARGUMENTS` is an optional task slug.

## Steps

1. **List tasks**: use `rdm_task_list` with `project: "<PROJECT>"` to see open and in-progress tasks.
2. **Show details**: if a task slug was provided in `$ARGUMENTS`, use `rdm_task_show` with `project: "<PROJECT>", task: "<slug>"`. Otherwise, present the task list and ask the user which task to work on.
3. **Mark in-progress**: use `rdm_task_update` with `project: "<PROJECT>", task: "<slug>", status: "in-progress"`
4. **Enter plan mode**: use the `EnterPlanMode` tool to switch into planning mode.
5. **Create an implementation plan** using the planning tool. The plan should:
   - Break the task into concrete implementation steps based on the task description
   - Include a final step: "Review changes with user and commit"
6. **Wait for user approval**: the user will review the plan and either accept or request changes. Do not proceed until the plan is accepted.
7. **Exit plan mode**: use the `ExitPlanMode` tool to switch back to execution mode.
8. **Execute the plan**: implement each step, following the plan.
9. **Review with user**: present a summary of the changes and ask the user to confirm they are ready to finalize.
10. **Finalize**: on user acceptance, commit the implementation changes with a `Done: task/<slug>` line in the commit message — the post-merge hook will mark the task done and record the commit SHA.
    **Use the exact task slug from the rdm tools you used earlier — do NOT invent or paraphrase it.**
    If the task is also part of a roadmap phase, include a `Done: <roadmap-slug>/<phase-stem>` line as well (using exact slugs/stems from rdm).
