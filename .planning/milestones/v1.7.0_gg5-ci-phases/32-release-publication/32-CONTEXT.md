# Phase 32: Release & Publication - Context

**Gathered:** 2026-10-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Publish `v1.7.0_gg5` on `custom_build`: `VERSION`, `CHANGELOG.md`, `docs/FORK-DELTA.md` and a short reviewed `docs/ROLLOUT-v1.7.0_gg5.md`; two green `--rerun` suite runs on `latest` × V2; one green `tests.yaml` run on the published SHA before the tags; then the nightly's first manual dispatch (NIT-03). The phase edits no `src/`, test, `build.gradle.kts`, `gradle/` or `.github/` file.

</domain>

<decisions>
## Implementation Decisions

### Tag and release mechanics
- **D-01:** Tags are hand-pushed by name, as in Phase 29: `v1.7.0_gg5` and `pre-v1.7.0_gg5`, only after the `custom_build` `tests.yaml` run is green. No `release.yaml` or `pre-release.yaml` dispatch and no GitHub release in this phase; Phase 30's dry run already proved the release path builds.
- **D-02:** `pre-v1.7.0_gg5` keeps the Phase 29 meaning: the `custom_build` tip before the merge, the rollback target.

### Rollout note
- **D-03:** `docs/ROLLOUT-v1.7.0_gg5.md` is one page with the five required claims only (empty `git diff v1.7.0_gg4 -- src/main`; wrapper-built archive verified on the release path; MBean `version=` and client name read `v1.7.0_gg5`; how the nightly is turned on and off; rollback). Each claim carries its measured command and output.
- **D-04:** Reviewer rounds and dispositions live in the phase directory's REVIEW files (as in Phase 31), not in the note. The note stays operator-facing.

### CHANGELOG and FORK-DELTA
- **D-05:** The `1.7.0_gg5` CHANGELOG section leads with "no connector code changes (`src/main` identical to `1.7.0_gg4`)", then bullets for the pinned release workflows with `gh release create`, the nightly, the CIG-01 and NIT-02 guards, and `dependabot.yml` only if it changed.
- **D-06:** FORK-DELTA gets one CI/workflows group. The "left as upstream ships them" paragraph (DEPS-F4) is replaced. Rows cover the release-path workflows, the nightly, both guards and any Dependabot change, each with its anchor. Raw numbers are refreshed once, on the final tree.

### Gates and nightly handoff
- **D-07:** Gate order follows Phase 29: docs written on the final tree; two `--rerun` suite runs, `testUnit` with Docker stopped, and the anchors gate on the release tip; re-fetch `origin`; one direct user approval; then push scratch, `--no-ff` merge from a throwaway worktree, push `custom_build`, wait for its single run, push tags, delete scratch. Gates run orchestrator-inline; `SELECT version()` is recorded at each.
- **D-08:** If the nightly's first manual run is red after the tags, the tags stay (a workflow-only release that passed `tests.yaml`). The failure goes back as a Phase 31 gap fix or a named skip-list entry, and `NIGHTLY_ENABLED` stays unset until a run is green.

### Claude's Discretion
- Exact layout of the REVIEW and command-evidence records, reviewer round count (at most three), and wording of the note and CHANGELOG bullets.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/ROADMAP.md` §Phase 32 and Sync Mechanics: goal, success criteria, five-step publication recipe, standing no-edit rule
- `.planning/REQUIREMENTS.md`: CIG-02, CIG-03, NIT-03 (NIT-03 moved here by D-02 of Phase 31)
- `.planning/phases/31-fork-owned-nightly-integration-tests/31-CONTEXT.md`: schedule gate (`NIGHTLY_ENABLED`), skip-list rules, failure policy
- `.planning/phases/30-release-workflows-pin-guard/30-CONTEXT.md`: release-path workflow decisions, dry-run record
- `docs/ROLLOUT-v1.7.0_gg4.md`: prior note, for style only
- `docs/FORK-DELTA.md`, `CHANGELOG.md`, `VERSION`, `.planning/scripts/fork-anchors.txt`: edited here; the `VERSION` anchor row changes in the same commit
- `.github/workflows/tests.yaml`, the nightly workflow, `integrationTests.yaml` (must stay disabled and byte-identical)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 29's publication recipe and command-evidence records; Phase 31's REVIEW and REVIEW-DISPOSITION files as the review-record template.

### Integration Points
- The anchors gate and `testUnit` are the cheap gates; the full suite runs twice with `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ... --rerun`.

</code_context>

<specifics>
## Specific Ideas

- Fork releases have no GitHub release object; only tags (`git tag` shows `v1.7.0_gg1`..`gg4` and `pre-` forms).

</specifics>

<deferred>
## Deferred Ideas

- A real `release.yaml` / `pre-release.yaml` dispatch to produce a GitHub release with the archive: not chosen; revisit if operators want a downloadable archive on GitHub.

</deferred>

---

*Phase: 32-release-publication*
*Context gathered: 2026-10-26*
