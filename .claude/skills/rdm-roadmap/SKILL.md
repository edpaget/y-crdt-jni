---
name: rdm-roadmap
description: Create an rdm roadmap with phases for a topic
allowed-tools:
  - Read
  - Glob
  - Grep
  - mcp__rdm__rdm_roadmap_create
  - mcp__rdm__rdm_phase_create
  - mcp__rdm__rdm_roadmap_show
---

Create an rdm roadmap with phases for the topic described in `$ARGUMENTS`.

## Steps

1. **Explore the codebase** to understand the current state relevant to `$ARGUMENTS`. Read key files, search for related code, and build context.
2. **Design phases** that break the work into independently deliverable increments. Each phase should produce a working, testable result.
3. **Create the roadmap**: use `rdm_roadmap_create` with `project: "<PROJECT>", slug: "<slug>", title: "Title", body: "Summary."`
4. **Create each phase** with context, steps, and acceptance criteria in the body:
   Use `rdm_phase_create` with `project: "<PROJECT>", roadmap: "<roadmap-slug>", slug: "<slug>", title: "Phase title", number: <n>, body: "<markdown body>"`

   The body should include:
   ```
   ## Context
   Why this phase exists and what it builds on.

   ## Steps
   1. First step
   2. Second step

   ## Acceptance Criteria
   - [ ] Criterion one
   - [ ] Criterion two
   ```
5. **Verify** the roadmap looks correct: use `rdm_roadmap_show` with `project: "<PROJECT>", roadmap: "<slug>"`

## Guidelines

- Aim for 2–6 phases per roadmap
- Each phase should be independently deliverable and testable
- Include Context, Steps, and Acceptance Criteria in every phase body
- Order phases so each builds on the previous one
- Use clear, descriptive slugs (e.g., `add-caching`, `migrate-auth`)
