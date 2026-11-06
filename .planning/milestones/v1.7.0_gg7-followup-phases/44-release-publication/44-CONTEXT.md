# Phase 44: Release & Publication - Context

**Gathered:** 2026-11-02
**Status:** Ready for planning
**Mode:** Auto-generated (discuss skipped via workflow.skip_discuss)

<domain>
## Phase Boundary

**Goal**: `v1.7.0_gg7` is published on `custom_build`. Operators get an independently reviewed rollout note that states what F-1, F-2 and the four RSK items change for them, with each claim labelled by its evidence. The gg7 allow-list compare passes on the final tree, and the suite is green twice. The published SHA has one green `tests.yaml` run before the user's publish run creates the tag. Every push, the merge and the publish run happen only on the user's direct word.

**Depends on**: Phases 39–43 (it documents and publishes their outcome)

**Requirements**: RLS-01, RLS-02, RLS-03, RLS-04

**Success Criteria** (what must be TRUE):

  1. *[Docker-free]* `VERSION` reads `v1.7.0_gg7`, set by `bump_version.py`, and the generated version agrees after `clean`. `CHANGELOG.md` has a `1.7.0_gg7` section, and `docs/FORK-DELTA.md` is final, with its raw numbers refreshed once. Every `fork-anchors.txt` row changes in the same commit as the count it changes (RLS-01).
  2. *[Docker-free]* `confluent-archive-allowlist-v1.7.0_gg7.txt` was generated last, from the final tree, with exact-path or literal-prefix rows and the gson rows. A dry run of the compare reads `0 unallowed, 0 stale rows` (RLS-02).
  3. *[record]* `docs/ROLLOUT-v1.7.0_gg7.md` covers F-1, F-2 and RSK-01..04, each claim labelled `measured`, `read from the code` or `derived`. For F-2 it states the retriable failure after the timeout, the duplicate risk with `exactlyOnce=false`, the `socket_timeout` override and the unbounded cases. Before the push gate, a fresh `gsd-code-reviewer` spawned by the orchestrator reviewed the note together with `CHANGELOG.md`, `docs/FORK-DELTA.md` and the published notes edited in Phase 43. Rounds repeated until one found nothing (at most three unless the user authorises more), and every finding's disposition is recorded (RLS-03).
  4. *[live]* On the release tip, two full `--rerun` suites pass on one pinned `latest` build with client V2, with `SELECT version()` and the named skips recorded. `testUnit` passes with Docker unreachable, and the anchors gate passes (RLS-04).
  5. *[operator, CI]* The user gives a direct approval, to the orchestrator and in their own words. After it, `custom_build` is a non-force `--no-ff` merge whose tree equals the rehearsed tip. The published SHA has exactly one `tests.yaml` run, and it is green before the user launches `release.yaml`. That publish run creates the tag `v1.7.0_gg7` and the GitHub release (RLS-04).

**Plans**: TBD

**Note**: This phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file (Phase 29 D-05). A defect found here goes back to its phase as a gap. Otherwise it is deferred with a reason, an owner and a re-open trigger in the milestone close record. The recipe is the gg6 one:
1. Check `origin/custom_build...sync/v1.7.0` for divergence before gating. A left count of 1 is the gg6 merge; more means `origin` moved.
2. Push the scratch branch once, at a SHA that no other pushed branch holds, and wait for green.
3. Merge with `--no-ff` from a throwaway worktree, so the merged tree equals the rehearsed tip.
4. Push `custom_build` only, and wait for its single run.
5. The user launches the publish run (`release.yaml`, `dry_run` not ticked). Push `pre-v1.7.0_gg7` by name after it.
6. Delete the scratch branch.

Before the gate, recheck upstream: tags past `v1.7.0`, and PRs #861, #855 and #857. The CHANGELOG heading carries the merge date in UTC. The approval gates run orchestrator-inline, because executors refuse relayed approvals and `custom_build` is the protected base branch.

</domain>

<decisions>
## Implementation Decisions

### Claude's Discretion
All implementation choices are at Claude's discretion — discuss phase was skipped per user setting. Use ROADMAP phase goal, success criteria, and codebase conventions to guide decisions.

</decisions>

<code_context>
## Existing Code Insights

Codebase context will be gathered during plan-phase research.

</code_context>

<specifics>
## Specific Ideas

No specific requirements — discuss phase skipped. Refer to ROADMAP phase description and success criteria.

</specifics>

<deferred>
## Deferred Ideas

None — discuss phase skipped.

</deferred>
