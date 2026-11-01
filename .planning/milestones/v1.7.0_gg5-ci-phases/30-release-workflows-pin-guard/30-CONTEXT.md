# Phase 30: Release Workflows & Pin Guard - Context

**Gathered:** 2026-10-24
**Status:** Ready for planning

<domain>
## Phase Boundary

The fork's three release-path workflows (`release.yaml`, `pre-release.yaml`, `increment-version.yml`) run current, SHA-pinned actions only. Both release workflows build with the project Gradle wrapper through `setup-gradle` (so the `verifyConfluentArchive` finalizer runs) and publish with `gh release create`, attaching the Confluent archive. A dry run proves both paths without a release or a tag. A Docker-free guard fails on any unpinned action in a fork-owned workflow. No connector code changes (`src/main` diff stays empty).

</domain>

<decisions>
## Implementation Decisions

### Dry-run mechanism (REL-05)
- **D-01:** Both `release.yaml` and `pre-release.yaml` get a boolean `workflow_dispatch` input `dry_run`. When true: build, run `verifyConfluentArchive`, `gh release create --draft` with the archive attached (this exercises the attach step), then delete the draft with `gh release delete --cleanup-tag` in an `if: always()` step. Research must confirm a draft creates no tag before this is relied on.
- **D-02:** `dry_run` defaults to `false`; dispatching without touching the form publishes exactly as today.
- **D-03:** A real publish runs only on `custom_build`: the publish step is gated on `!inputs.dry_run && github.ref == 'refs/heads/custom_build'`. A scratch-branch dispatch can only dry-run.
- **D-04:** The dry-run steps live once per workflow (no duplicated job); real and dry paths differ only by the `--draft` flag and the cleanup step.

### Tag ownership and permissions
- **D-05:** `gh release create <tag> --target $GITHUB_SHA` creates the tag. The separate `git tag` / `git push` step in `release.yaml` is deleted. `pre-release.yaml` uses the same step with `--prerelease`; `release.yaml` does not.
- **D-06:** The workflows enforce "tags reach origin only after the published SHA has a green `tests.yaml` run": before a real publish, query the Actions API for a successful `tests.yaml` run on `$GITHUB_SHA` and fail if none. Skipped on dry runs.
- **D-07:** Per-job least privilege: top-level `permissions: contents: read`; the release job escalates to `contents: write` plus `actions: read` (for D-06). Checkout uses `persist-credentials: false`.

### increment-version.yml
- **D-08:** Pin only; do not fix the likely `semver.inc('v1.7.0_gg4', ...)` → `vnull` defect. Record it in FORK-DELTA as known/unfixed (the fork bumps `VERSION` by hand). Before recording, run a one-line node check to confirm the defect and record its output in the phase (it was read from the grammar, not run).
- **D-09:** Replace Node 16 with a current LTS via a SHA-pinned current `setup-node` major (research picks exact versions). Drop the pointless `npm install fs`.

### Dependabot and guard scope
- **D-10:** User signs off on editing the pinned assertion: drop `increment-version.yml`, `pre-release.yaml`, `release.yaml` from `dependabot.yml` `exclude-paths`, keep `integrationTests.yaml`, and update `UPSTREAM_WORKFLOWS` in `TestsWorkflowPinGuardUnitTest` to match. The reason is recorded in the same commit.
- **D-11:** CIG-01 guard is a NEW class under `src/test/java/com/clickhouse/kafka/connect/ci/` (e.g. `WorkflowActionPinGuardUnitTest`). It discovers `.github/workflows/*` from the directory, excludes only upstream-identical `integrationTests.yaml`, reuses the `PINNED_USES` regex shape (40-hex SHA + `# vX.Y.Z`), names file and line on failure, and carries red-on-fixture tests per unpinned form (tag, branch, short SHA, missing comment). `TestsWorkflowPinGuardUnitTest`'s tests.yaml-specific rules are left alone apart from D-10.
- **D-12:** The guard reads workflows only, not `.github/actions/*` composites (`use-clickhouse-java` is not called by any fork workflow). Record this in the guard's javadoc.

### Claude's Discretion
- Exact action versions/SHAs: reuse `tests.yaml`'s pins (checkout v7.0.1, setup-java v6.0.1 with `temurin`, setup-gradle v6.4.0 by peeled commit); research picks `setup-node` and the Node LTS.
- Exact shape of the Actions API query for D-06 and the guard's fixture layout.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Planning
- `.planning/ROADMAP.md` §Phase 30 — goal, success criteria, research flags, notes (dispatch is operator-only; scratch-branch push needs direct approval; re-read `git ls-remote origin custom_build` first)
- `.planning/REQUIREMENTS.md` — CIG-01, REL-01..REL-05

### Code to change
- `.github/workflows/release.yaml`, `.github/workflows/pre-release.yaml`, `.github/workflows/increment-version.yml` — the three files to pin/rewrite
- `.github/workflows/tests.yaml` — source of the action pins to reuse
- `.github/workflows/integrationTests.yaml` — upstream-identical, excluded from the guard
- `.github/dependabot.yml` — `exclude-paths` edit (D-10)
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` — `PINNED_USES`, `UPSTREAM_WORKFLOWS`, nonCommentLines helpers; style template for the new guard
- `build.gradle.kts` (~L440-466) — `createConfluentArchive` finalized by `verifyConfluentArchive`
- `docs/FORK-DELTA.md` — where the increment-version defect is recorded; anchors invariant applies to new workflow-edit rows

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TestsWorkflowPinGuardUnitTest` (`PINNED_USES`, `USES_LINE`, UTF-8 line reading, `Optional<String>` problem-returning checks): model for the new guard.
- `tests.yaml` SHA pins with `# vX.Y.Z` comments.

### Established Patterns
- Docker-free guards extend nothing (no `ClickHouseBase`) so they stay in `testUnit`; files read as UTF-8 explicitly; no YAML library, line-level checks.
- Guard goes red on fixtures first and lands green in the same commit as the pins.
- Anchor rows for workflow edits land in the same commit as the edit.

### Integration Points
- `.github/dependabot.yml` github-actions entry; fork-anchors invariant; Phase 31's nightly must pass this guard unchanged.

</code_context>

<specifics>
## Specific Ideas

- Both release files are named `Release`; consider distinguishing run names (e.g. `run-name`) so Actions lists are readable.

</specifics>

<deferred>
## Deferred Ideas

- Fixing `increment-version.yml` for the `vX.Y.Z_ggN` scheme, or retiring it — a future phase if the fork ever wants automated bumps.

</deferred>

---

*Phase: 30-Release Workflows & Pin Guard*
*Context gathered: 2026-10-24*
