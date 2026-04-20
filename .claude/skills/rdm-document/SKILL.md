---
name: rdm-document
description: Generate user documentation from a completed rdm roadmap
allowed-tools:
  - Read
  - Glob
  - Grep
  - Write
  - Edit
  - mcp__rdm__rdm_roadmap_show
  - mcp__rdm__rdm_phase_show
---

Generate user-facing documentation from a completed rdm roadmap. `$ARGUMENTS` should be `<roadmap-slug> [--out <path>]`.

## Steps

1. **Parse arguments**: extract the roadmap slug and optional `--out <path>` from `$ARGUMENTS`. Default output path is `docs/<slug>.md`.
2. **Read the roadmap**: use `rdm_roadmap_show` with `project: "<PROJECT>", roadmap: "<slug>"` to get the overview and phase list.
3. **Validate completion**: all phases must be `done`. If any phase is not done, abort with a clear message listing incomplete phases.
4. **Read each phase**: use `rdm_phase_show` with `project: "<PROJECT>", roadmap: "<slug>", phase: "<stem>"` for each phase. Collect titles, bodies, and commit SHAs.
5. **Gather code changes** from commit SHAs (the `commit` field in phase output):
   - If SHAs are available: `git log --oneline <first_sha>~1..<last_sha>` and `git diff --stat <first_sha>~1..<last_sha>` in the source repo
   - Single commit: `git show --stat <sha>`
   - Missing SHAs: warn and fall back to phase descriptions only — do not abort
6. **Cross-reference** phase descriptions with actual code changes to ensure accuracy.
7. **Draft documentation** structured as:
   - **Overview** — what the feature is
   - **Motivation** — why it was built
   - **Usage** — concrete examples (CLI commands, config options)
   - **How it works** (optional) — architecture/internals for complex features
   - **Limitations** (optional) — known gaps
8. **Write** the documentation to the output path.
9. **Present the draft** to the user for review before considering done.

## Guidelines

- Write for users, not developers — focus on what they can do
- The Usage section is the most important — include real, working examples
- Internal/refactoring phases: mention in "How it works" if relevant, omit from Usage
- Derive content from both phase descriptions (intent) and code diffs (what shipped)
- If phases lack commit SHAs, note which ones and rely on descriptions alone
