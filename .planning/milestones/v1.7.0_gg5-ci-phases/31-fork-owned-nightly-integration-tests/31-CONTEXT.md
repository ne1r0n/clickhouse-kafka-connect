# Phase 31: Fork-Owned Nightly Integration Tests - Context

**Gathered:** 2026-10-26
**Status:** Ready for planning

<domain>
## Phase Boundary

A fork-owned nightly workflow runs the `integrationTest` task on ClickHouse `latest` × client V2 in the runner's own Docker, with no cloud job and no cloud secrets. Upstream's `integrationTests.yaml` stays byte-identical and disabled, pinned by a Docker-free guard. A measured local `integrationTest` run precedes the workflow. No connector code changes (`src/main` diff stays empty).

</domain>

<decisions>
## Implementation Decisions

### Schedule gate and NIT-03 timing
- **D-01:** The nightly has `workflow_dispatch` and a `schedule` trigger. The schedule is gated by a repository variable: `if: github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`. Manual dispatch always works; only cron runs are gated. The gate is off at the Phase 32 merge, and the user sets the variable after the dispatch run. Phase 32 edits no `.github/` file, so enabling is an operator setting.
- **D-02:** NIT-03 (the dispatch run) moves to Phase 32 if research confirms GitHub dispatches only workflow files present on the default branch (`custom_build`). The user chose this over a temporary scratch-branch `push` trigger or early publication. Phase 31 then ends with measurement, workflow, guard and a record. REQUIREMENTS.md and ROADMAP.md must be updated for the move, with the user's confirmation at that point, since NIT-03 is mapped to Phase 31 today.
- **D-03:** Research must confirm the dispatch behaviour first; D-02 applies only if confirmed.

### Test scope
- **D-04:** The nightly runs the whole `integrationTest` task (all three classes). `ClickHouseCloudTest` self-skips without cloud properties, and the measurement confirms it skips rather than fails. Upstream's standalone filter is not used.
- **D-05:** The job timeout is set from the local measurement with headroom (about 1.5–2×), recorded with the measurement. No test is dropped for time alone, and the job is not split per class.
- **D-06:** Testcontainers pulls the Confluent and ClickHouse `latest` images at run time. The measurement records the image list and tags, and `latest` drift is accepted and recorded.

### Skip list
- **D-07:** If any skip list is needed, it is a fork-owned exclude in the build (`build.gradle.kts` or a small included file) applied to `integrationTest`, so local and CI runs behave identically. It is not in workflow Gradle filters and not `@Disabled` in test source.
- **D-08:** Each entry is a fully-qualified test name (class#method) with a cause category (environment, harness, cloud-only, connector defect), a one-line reason, and a re-open trigger. Names only, no patterns. A guard may check that entries are names. No existing test is deleted or weakened.

### Failure policy and reporting
- **D-09:** A connector-defect failure (needs `src/main` change) stops for a user decision. The expected outcome is a skip-list entry with category `connector defect` and a deferred-fix record. No `src/main` edit in this milestone. A harness fix under `src/integrationTest` or `src/testFixtures` is allowed with its reason recorded; an existing assertion is never weakened.
- **D-10:** On failure the nightly uploads test reports and writes the failing test names to `GITHUB_STEP_SUMMARY` plus an annotation, since job logs are not readable without admin rights. No issue automation, so `permissions: contents: read` stays.

### Claude's Discretion
- Cron time (must not fire before the gate is enabled; the gate guarantees that), concurrency group, artifact names and retention.
- Exact shape of the `integrationTests.yaml` byte guard (e.g. expected-bytes hash or embedded copy), provided it was seen red on a one-byte edit and covers NIT-02's "disabled" state through the Actions API check recorded in the phase.
- How the measurement record is laid out, within criterion 1's required contents.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Planning
- `.planning/ROADMAP.md` §Phase 31 — goal, success criteria, research flags, notes
- `.planning/REQUIREMENTS.md` — NIT-01..NIT-03 and the note that NIT-03 may move to Phase 32
- `.planning/phases/30-release-workflows-pin-guard/30-CONTEXT.md` — CIG-01 guard (D-11), action pins, least-privilege permissions pattern

### Workflows and guards
- `.github/workflows/integrationTests.yaml` — upstream-identical, disabled; must stay byte-identical; its cloud/standalone/cluster job shapes are the reference for what upstream runs
- `.github/workflows/tests.yaml` — SHA pins to reuse (checkout v7.0.1, setup-java v6.0.1 `temurin`, setup-gradle v6.4.0, upload-artifact v7.0.2), `ubuntu-24.04`, 45-minute budget, `persist-credentials: false`
- `src/test/java/com/clickhouse/kafka/connect/ci/TestsWorkflowPinGuardUnitTest.java` — `UPSTREAM_WORKFLOWS`, style template for the new byte guard
- `src/test/java/com/clickhouse/kafka/connect/ci/` — Phase 30's `WorkflowActionPinGuardUnitTest`; the nightly must pass it unchanged
- `.github/dependabot.yml` — the nightly must not be added to `exclude-paths`

### Build and tests
- `build.gradle.kts` (~L170, ~L388) — `integrationTest` suite and `tasks.named<Test>("integrationTest")` config; home of any skip-list exclude
- `src/integrationTest/java/com/clickhouse/kafka/connect/sink/` — `ClickHouseCloudTest`, `ClickHouseSinkConnectorIntegrationTest`, `ExactlyOnceTest`, `helper/`
- `docs/FORK-DELTA.md` — anchors invariant; new workflow/guard rows land with the change

### Memory-recorded context
- Nightly Integration Tests inherited red (upstream fails 43/43 daily; standalone `latest` × V2 run 38881153720) — explain this in the measurement record
- Colima profile `vz` for local Docker; OpenJ9 Mockito JVM crash notes if the test JVM exits 255

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `tests.yaml`: pinned-action job skeleton (checkout, setup-java, setup-gradle, report uploads) to copy for the nightly.
- `TestsWorkflowPinGuardUnitTest`: file-reading, UTF-8, `Optional<String>` problem-returning helpers for a byte-identity guard.
- `ClickHouseTestHelpers` / `ClickHouseBase`: environment variables `CLICKHOUSE_VERSION` and `CLIENT_VERSION` select the image and client.

### Established Patterns
- Docker-free guards extend nothing and stay in `testUnit`; each guard is seen red on a fixture before it lands green.
- Measurement precedes the workflow; operator-only steps (dispatch, variable setting) are performed by the user.
- Anchor rows land in the same commit as the edit.

### Integration Points
- Phase 30's pin guard auto-discovers the new workflow.
- Phase 32 publishes it with the gate off and edits no `.github/` file.

</code_context>

<specifics>
## Specific Ideas

- Gate expression: `github.event_name == 'workflow_dispatch' || vars.NIGHTLY_ENABLED == 'true'`.
- Skip-list entry form: test name + cause category + reason + re-open trigger.

</specifics>

<deferred>
## Deferred Ideas

- Issue automation on nightly failure (needs `issues: write`) — not chosen; revisit if failed-run emails prove insufficient.
- Pinning the ClickHouse image digest — contradicts the `latest` target; not chosen.

</deferred>

---

*Phase: 31-fork-owned-nightly-integration-tests*
*Context gathered: 2026-10-26*
