# Phase 14: Dependency Bumps & CI - Context

**Gathered:** 2026-10-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Land the five user-approved dependency bumps on the hardened tree (after Phase 13), **one attributable commit per version change**, so that a regression points at exactly one bump:

- lz4-java past upstream's `1.10.4` (≥ 1.11.2, target 1.12.0)
- Jackson `2.22.3`
- okhttp `5.5.0`
- org.json `20260814`
- mockito `5.24.0`

Fix `tests.yaml` in three ways:
- Compile the integration-test source set on every push (CI-10).
- Surface CI's test fork count (CI-09).
- Make a published SHA get exactly one completed run (CI-08). This fix must be committed before any tag reaches `origin`.

Requirements: BLD-10, BLD-11, BLD-12, CI-08, CI-09, CI-10.

**Not in this phase:**
- Pushing any tag.
- Touching `custom_build`.
- `VERSION`, CHANGELOG and FORK-DELTA prose. That is Phase 15, apart from evidence notes this phase records for it.
- Client `0.10.0`.
- Phase 15's two-run SUITE-04 gate.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- The bump set and versions were user-approved on 2026-10-07 (REQUIREMENTS locked decisions). They are applied as **plain version edits** in `gradle/libs.versions.toml`, never as dependabot cherry-picks.
- ClickHouse client stays `0.9.6`. `kafka = "4.1.1"` stays. `0.10.0` is out of scope.
- No tag is pushed in Phase 14. Phase 15 proves CI-08 end to end on the published SHA.
- Main-code changes outside a phase's declared scope stop for a user decision. Existing tests are never deleted or weakened. Type coverage asserts typed SQL read-back, never `countRows`.
- Always invoke Gradle as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. Run `spotlessApply` only on touched files.
- `verify-merge-invariants.sh` runs at the end of the phase (Phase 12 D-09). Any commit that changes a `fork-anchors.txt` anchor updates the file in the same commit (Phase 12 D-08). Note that `tests.yaml	timeout-minutes: 45	1` is an anchor.

### lz4-java (BLD-10)
- **D-01: The "live LZ4 round-trip" is a committed live test using a test-only client-v2.**
  - The client is built with `compressClientRequest(true)` and `compressServerResponse(true)` (LZ4).
  - It inserts rows into ClickHouse `latest` and reads them back with typed SQL `assertEquals`.
  - Rationale: the connector's own clients never run lz4-java. Since Phase 12 they set `compress=false`/`compressServerResponse(false)`, and `network_compression_method=lz4` is a server setting. Only an explicitly compressing client exercises the jar in both directions.
  - Connector defaults and main code are untouched.
  - The test stays committed so it guards the next lz4 or client bump.
- **D-02: Test first, then bump.**
  - Commit 1 adds the round-trip test, green on the current `1.10.4`.
  - Commit 2 is a pure `lz4Java` version edit.
  - A failure in commit 2 is then attributable to the version alone.
- **D-03: Fallback ladder.** If `1.12.0` fails with client `0.9.6` (compile, Docker-free `testUnit`, or the D-01 live round-trip):
  - Try the newest advisory-free `1.11.x` (≥ 1.11.2).
  - If that also fails, **stop at a blocking checkpoint**.
  - Record which version failed, at which gate, and the error text.
- **D-04: Evidence for "advisory-free" is a recorded lookup.**
  - Research checks GitHub Advisory DB / OSV for `at.yawk.lz4:lz4-java` and `org.lz4:lz4-java`, including the known lz4-java CVEs that motivated "≥ 1.11.2".
  - It records the source, the date and the result in the phase artifacts.
  - `dependencyInsight` must show exactly one `at.yawk.lz4:lz4-java` on `runtimeClasspath`, at the chosen version. Show `clickhouseDependencies` too, as BLD-09 did.

### Bump breakage policy (BLD-11, BLD-12)
- **D-05: Any bump that turns an existing test red stops at a blocking checkpoint.**
  - The checkpoint shows the failing test, the diagnosed cause and the options: adapt the test, hold the bump back, or change main code. The user picks.
  - No fix-forward, even for test-only causes such as mockito strict stubbing.
  - **Reversibility:** reversible. This is process only.
- **D-06: The org.json bump is gated by the full live suite on `latest` × V2**, not just `testUnit` plus compile.
  - Compare the `Int256`/`UInt256`/`Decimal256` results before and after. org.json 20260719 added a configurable BigInteger/Decimal length limit with an unknown default.
  - If a result changes, bisect by reverting `orgJson` alone (roadmap note), then go to D-05.
- **D-07: The Jackson `2.22.3` commit is gated by the full live suite on `latest` × V2.**
  - Jackson sits under the R-01 metadata-free JSON route, `StructToJsonMap`, `DataJson` and the schemaless path. Only a full run covers every consumer, and it includes Phase 13's R-01 parity matrix.
  - Jackson gets its own commit (BLD-11).
- **D-08: okhttp and mockito use the roadmap's minimum gate:** `compileIntegrationTestJava` plus Docker-free `testUnit`, one commit each.
- **D-09: The phase gate is one full live-suite run on the phase's final commit.**
  - Pass criteria: 0 failures, 0 errors, and a skip set equal to the 18 named skips.
  - Also required: Docker-free `testUnit` and `verify-merge-invariants.sh`.
  - Phase 15 still runs its own two runs (`--rerun`).

### CI-08: trigger and concurrency
- **D-10: `tests.yaml` triggers on branch pushes only** (`on: push: branches: ['**']`), so a tag push never starts a run.
  - The SHA-keyed concurrency group and `cancel-in-progress: true` stay.
  - Release workflows are already `workflow_dispatch`, so nothing depends on tag-triggered tests.
  - Residual risk, recorded rather than fixed: two *branches* pushed at the same SHA still share a group and cancel each other. Phase 15 must push the published SHA on one branch only.
  - The fix is committed before the D-13 push and before any tag reaches `origin`.
  - **Reversibility:** costly. Phase 15's "exactly one run" proof and every later publication depend on it, and tag pushes no longer run tests.
- **D-11: CI-08 evidence in this phase has two parts:**
  - (a) A written note in the phase artifacts explaining why a tag push cannot create a `tests.yaml` run under the new trigger filter.
  - (b) The fixed workflow observed running exactly once on the pushed scratch-branch SHA (D-13), located by `head_sha` plus workflow path, with no cancelled sibling.
  - Phase 15's published-SHA check remains the end-to-end proof.

### CI-09 / CI-10 step shape
- **D-12: Use separate, named steps in `tests.yaml`, with no `build.gradle.kts` change.**
  - A step prints `nproc` and the derived `maxParallelForks` (`nproc/2`, minimum 1, mirroring `build.gradle.kts`) to the job log and to `$GITHUB_STEP_SUMMARY`.
  - A dedicated `./gradlew compileIntegrationTestJava` step runs before `Run tests`, so a failure names its step.
  - CI-09 is satisfied by reading the value from that run's log or summary.

### Pushing `sync/v1.7.0` (amends Phase 12 D-01)
- **D-13: Push `sync/v1.7.0` to `origin` once, behind a blocking human-approval checkpoint.**
  - Preconditions: all bump and CI commits have landed and the D-09 local gates are green.
  - The executor never pushes on its own initiative.
  - Push the branch only: no tag, and `custom_build` untouched.
  - Record this as an amendment to Phase 12 D-01, which kept the branch local through Phases 12–14.
  - Before pushing, confirm that `.planning/.gsd-ws-arg` is not tracked and that no local-only tag rides along.
  - **Reversibility:** one-way. It publishes the Phase 12–14 code and planning commits to the public fork. Deleting the remote branch afterwards does not unpublish them.
- **D-14: If the pushed run fails while local gates were green, diagnose and stop.**
  - Read the job log through the unauthenticated GitHub REST API (`gh` is not installed, by design).
  - Classify the failure as environment/flake or a real defect.
  - Stop at a checkpoint with the diagnosis.
  - Any fix is a new commit plus another push, both approved by the user.

### Claude's Discretion
- The **order of the bump commits**, subject to:
  - D-02, so the lz4 test lands before the lz4 bump.
  - The CI-08 fix being committed before the D-13 push.
  - Live-gated bumps (Jackson, org.json) being placed so each gets its own full run.
  - Possibly putting the CI-10 compile step first, so CI checks okhttp from the start.
- Whether `jacksonAnnotations = "2.22"` needs to change. Jackson annotations use major.minor versioning; research confirms whether `2.22` already pairs with `2.22.3`.
- Whether to drop the dead `pull_request` branch of the concurrency-group expression. It is harmless and keeps the diff close to upstream; leaving it alone is fine.
- The D-01 test's class name, location, column types and row count. It should sit beside `NetworkCompressionPinLiveTest` on the shared container, tagged `clickhouse`.
- Where Phase 14 records its evidence: the advisory lookup, `dependencyInsight` output, before/after Int256/Decimal256 comparison, the CI run URL and the fork count. Phase 15 turns these into CHANGELOG/FORK-DELTA prose (the lz4 version row in FORK-DELTA §BLD-09 and the CHANGELOG lz4 line).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and locked decisions
- `.planning/ROADMAP.md` §"Phase 14: Dependency Bumps & CI": goal, the four success criteria, and the org.json and scratch-branch-push notes.
- `.planning/REQUIREMENTS.md`: the locked-decisions table at the top (dependency bumps and the lz4 target), and BLD-10..12 and CI-08..10.
- `.planning/STATE.md` §Blockers/Concerns: the `TESTCONTAINERS_HOST_OVERRIDE` env quirk, no `gh` CLI (REST API only), and the CI tag/branch cancel incident on `c61d90ce`.
- `.planning/phases/12-upstream-v1-7-0-merge-reconcile/12-CONTEXT.md`: D-01 (branch stays local, amended here by D-13), D-08 (anchor file updated in the same commit) and D-09 (invariant script every phase).

### Build and dependencies
- `gradle/libs.versions.toml`: `lz4Java`, `jackson`, `jacksonAnnotations`, `okhttp`, `orgJson`, `mockito` version lines. `kafka = "4.1.1"` stays.
- `build.gradle.kts`: the upstream `org.lz4:lz4-java` capability rule (around lines 60–71). `maxParallelForks = availableProcessors/2` (around line 204). The `testUnit` task (around line 249). The `integrationTest` suite, where okhttp is used (around lines 160–170).
- `docs/FORK-DELTA.md`: the BLD-09 lz4 capability row, and the compression row (which explains why connector clients don't use LZ4).
- `.planning/MERGE-CHECKLIST-v1.7.0.md`: Recorded Facts 19, 24, 25 and 26 (lz4 and compression evidence). New facts continue the global numbering.

### CI
- `.github/workflows/tests.yaml`: the workflow to fix (trigger, concurrency, steps).
- `.github/workflows/*.yaml`: the release, pre-release and increment workflows are `workflow_dispatch`, and the integration/test-head workflows are `schedule`, so none needs a tag-triggered `tests.yaml`.
- `.planning/scripts/fork-anchors.txt`: line 40 anchors `tests.yaml	timeout-minutes: 45	1`. Lines 46–49 hold the build and catalog anchors.
- `.planning/scripts/verify-merge-invariants.sh`: the phase-end gate.

### Tests to reuse
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinLiveTest.java`: the live compression test on the shared container, the closest pattern for D-01.
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/NetworkCompressionPinUnitTest.java`: the Docker-free compression pins.
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseBase.java`: shared-container base class.
- `src/testFixtures/.../ClickHouseTestHelpers.java`: test utilities (read it before writing tests, per AGENTS.md).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NetworkCompressionPinLiveTest` already builds live clients against `latest` on the shared container and asserts compression behaviour. The D-01 round-trip test can follow its setup and helper usage.
- `ClickHouseBase` plus the `clickhouse` JUnit tag give a shared-container live test that `testUnit` excludes automatically.
- `.github/scripts/jacoco_summary.py` already writes to the job summary. The CI-09 step can follow the same `$GITHUB_STEP_SUMMARY` convention.

### Established Patterns
- Version catalog edits are one line each, and BLD-09/13 already established catalog-only commits.
- Seen-RED evidence and Recorded Facts go into the checklist and the phase SUMMARY, not into committed tests that can drift with `latest`.
- CI runs are checked through the unauthenticated REST API (`actions/runs?head_sha=`, `/jobs`, `/artifacts`; 60 requests per hour).

### Integration Points
- `ClickHouseHelperClient` builds connector clients with `compressServerResponse(false)` (around line 192). D-01 deliberately bypasses it with its own client builder, so main code stays untouched.
- `lz4-java` reaches the Confluent archive through `clickhouseDependencies`. Jackson ships there too (runtime). okhttp, org.json and mockito are test or integration-test only.

</code_context>

<specifics>
## Specific Ideas

- The tag/branch cancel actually happened on `c61d90ce`. Phase 15 locates the published run by `head_sha` plus workflow path with no cancelled sibling, so D-10 has to make that check pass by construction.
- The roadmap's org.json note is taken literally. If an Int256/Decimal256 fixture changes, bisect by reverting `orgJson` alone.

</specifics>

<deferred>
## Deferred Ideas

- Guarding against two *branches* pushed at the same SHA (the residual CI-08 risk under D-10) is not fixed here. It is recorded as an operational rule for Phase 15: push the published SHA on one branch only.
- Client `0.10.0`: unchanged, still a future requirement.

</deferred>

---

*Phase: 14-dependency-bumps-ci*
*Context gathered: 2026-10-09*
