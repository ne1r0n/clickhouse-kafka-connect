# Phase 17: CI, Build & Test Hygiene - Context

**Gathered:** 2026-10-11
**Status:** Ready for planning

<domain>
## Phase Boundary

`tests.yaml` becomes a SHA-pinned, least-privilege, Node 24 workflow on `ubuntu-24.04` whose fork-count step reads `maxParallelForks` from a Gradle task. `test-head.yaml` stops being a dead nightly. Dependabot covers the pinned actions. The Phase 14/15 build and test debt is closed: the Lombok Jackson2/3 warning (BLD-14), the duplicate `org.lz4:lz4-java` on `testRuntimeClasspath` (DEBT-07, backlog 999.2), the unasserted LZ4 response leg (DEBT-06), and the NanoTimestamp scientific-notation row (DEBT-08). One approved branch-only push with exactly one green `tests.yaml` run proves it (CI-17).

Requirements: CI-11, CI-12, CI-13, CI-14, CI-15, CI-16, CI-17, BLD-14, DEBT-06, DEBT-07, DEBT-08.

No `src/main` change. Pinning and other edits are scoped to `tests.yaml`. The other upstream workflows (`release`, `pre-release`, `increment-version`, `integrationTests`) stay untouched; upgrading them is a Future Requirement.

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked earlier; not re-opened)
- Pin every action to a full commit SHA with a `# vX.Y.Z` comment. Re-resolve each SHA at execution time. If `object.type == "tag"`, peel it to the commit; `gradle/actions` is an annotated tag. Check each SHA against its tag, not just that it resolves (the impostor-commit risk). Research values as of 2026-10-10: checkout v7.0.1, upload-artifact v7.0.2, setup-java v6.0.1, gradle/actions v6.4.0.
- `fork-anchors.txt` row `.github/workflows/tests.yaml  timeout-minutes: 45  1` must survive. Any commit that changes an anchor updates `fork-anchors.txt` in the same commit.
- `build.gradle.kts`:
  - Never repeat the literal `withCapability("org.lz4:lz4-java")`; it is anchored at count 1.
  - Never use `configurations.all { exclude }`.
  - `runtimeClasspath` and `integrationTestRuntimeClasspath` must resolve exactly as before.
- Lombok: append `lombok.jacksonized.jacksonVersion += 2` to the root `lombok.config`. Use `+=` (the key is a list) and `2` (the runtime mapper is Jackson 2). Prove it with a `javap` diff of `ClickHouseFieldDescriptor`'s generated code. `lombok.addLombokGeneratedAnnotation` must stay in effect.
- Process rules:
  - The CI-17 push needs the user's own explicit answer at an orchestrator-inline gate. A relayed approval is refused.
  - A red run is diagnosed and the work stops (Phase 14 D-14, Phase 16 D-11): no blind re-run, attribute the failure first, and a second run only with the user's OK.
  - CI is read through the unauthenticated GitHub REST API; `gh` is not installed.
- Existing tests are never deleted or weakened. Gradle always runs as `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew ...`. `spotlessApply` runs only on touched files. A two-run gate uses `--rerun`.

### test-head.yaml (CI-16)
- **D-01:** **Delete `.github/workflows/test-head.yaml`** from the fork.
  - Both of its jobs call `tests.yaml` as a reusable workflow, with inputs (`clickhouse_version: head`, `clickhouse_java: main`, `run_cloud_tests`) that the fork's latest-only `tests.yaml` does not declare.
  - Testing ClickHouse head and clickhouse-java main is outside the `latest` × V2 target (CI-01..04).
  - Do not add `workflow_call` to `tests.yaml`.
  - CI-16 is proven statically in this phase (the file is absent, and nothing else references it). The effect on the nightly schedule becomes observable only after Phase 20 publishes the deletion to `custom_build`.
  — **Reversibility:** costly. A future upstream sync hits a modify/delete conflict on this file every time it changes upstream; D-02 covers that.
- **D-02:** Guard against a sync quietly restoring the file:
  - A `docs/FORK-DELTA.md` row records the deletion and its reason.
  - `.planning/MERGE-CHECKLIST-v1.7.0.md` gets one line: "keep `test-head.yaml` deleted on modify/delete".
  - No new invariant-script logic.

### Dependabot and pin maintenance (CI-15, CI-11)
- **D-03:** Add a `package-ecosystem: "github-actions"` entry to `.github/dependabot.yml`, scoped to `tests.yaml` only by using `exclude-paths` on the four upstream workflows.
  - Research must confirm that `exclude-paths` is supported for the `github-actions` ecosystem.
  - If it is not, fall back to a plain `directory: "/"` entry covering all workflows, and record that fallback (and that bump PRs for upstream workflows are to be closed, not merged) in FORK-DELTA.
- **D-04:** The cadence matches the existing gradle entry: `interval: "monthly"`, label `dependencies`, `open-pull-requests-limit: 5`. A `groups` block bundles all action bumps into one PR, so the pins move together and get one CI run. The gradle entry is left untouched.
- **D-05:** CI-15 is met by the config file alone.
  - Research checks, via the API or the docs, whether Dependabot version updates actually run on this fork, or are switched off in its settings.
  - If enabling them is a settings toggle, add it to the Phase 20 operator steps as a manual check for the user. Make no settings change from here.
- **D-06:** Add a **Docker-free JUnit guard in `testUnit`** that reads `.github/workflows/tests.yaml` and fails unless:
  - every `uses:` line is pinned to `@[0-9a-f]{40}` followed by a `# vX.Y.Z` comment;
  - `runs-on` is `ubuntu-24.04` and no `ubuntu-latest` is left;
  - `permissions` has `contents: read` and checkout has `persist-credentials: false`.

  Use a plain regex or line scan, with no new YAML dependency. Additive: a new test class.

### LZ4 response-leg proof (DEBT-06)
- **D-07:** The primary assertion is `ProfileEvents['NetworkSendBytes']` against `result_bytes` from `system.query_log`, for **client-v2's own read query id** in `Lz4RoundTripLiveTest`.
  - This checks the client under test, which is the gap 14-REVIEW WR-01 named.
  - "Compressed" means LZ4 here because the session asserts `network_compression_method=lz4` (existing assertion around L128-130).
  - The research flag's short spike confirms that the read path works through client-v2 (explicit query id, `SYSTEM FLUSH LOGS`, the `QueryFinish` row).
  - Fallback 1: the raw `java.net.http` LZ4 method-byte (`0x82`) check from ARCHITECTURE.md.
  - Fallback 2, if neither works: narrow the test's `@DisplayName` and Javadoc to "request leg verified, response leg by read-back". Never leave the "both legs" claim unbacked.
- **D-08:** Use a **wide ratio bound**, e.g. `NetworkSendBytes < result_bytes / 10`.
  - LZ4 measured about 130× smaller and no compression about 1×, so the bound tolerates HTTP framing and payload tweaks and still fails clearly when compression is off.
  - Log the measured ratio on every run.
  - Do not try to tell LZ4 from ZSTD by magnitude.
- **D-09:** Prove "seen red" with a **one-off local flip**:
  - Temporarily set `compressServerResponse(false)` and run the test.
  - Record the command, the assertion message and the measured bytes in the phase evidence file.
  - Revert without committing.
  - No committed negative-control test.

### CI-17 push
- **D-10:** Push **once, at the end of the phase**: after all 11 requirements are implemented and the local gates are green (`testUnit`, a full live suite on `latest` × V2, `verify-merge-invariants.sh` anchors). The single green run then covers the new workflow and every Phase 17 build/test change together.
- **D-11:** Push **`sync/v1.7.0`** (the Phase 14 D-13 precedent):
  - Use an explicit refspec, push the branch only, with no tag. `custom_build` is untouched.
  - The SHA must differ from `origin/custom_build`'s.
  - Delete the branch from `origin` once its run is green. `origin/sync/v1.7.0` does not exist right now.
  - The push also publishes the local Phase 16 bookkeeping tail and the Phase 17 commits; that is accepted.
  - Before pushing, confirm that `.planning/.gsd-ws-arg` is not tracked and that no local-only tag rides along.
  — **Reversibility:** one-way. It publishes the Phase 16 tail and the Phase 17 commits to the public fork; deleting the remote branch afterwards does not unpublish them.
- **D-12:** **No stop-and-ask at 2026-10-19.** `tests.yaml` is pinned to `ubuntu-24.04`, so the `ubuntu-latest` switch does not affect this run. Push when ready. Phase 16 D-12's deadline rule existed only because that run used the unchanged `ubuntu-latest` workflow.
- **D-13:** Acceptance for the run (criterion 3):
  - exactly one `tests.yaml` run on the SHA (by `head_sha` plus workflow path) with no cancelled sibling;
  - the `ubuntu-24.04` image in the "Set up job" runner details;
  - only `Contents: read` / `Metadata: read` in the GITHUB_TOKEN permissions;
  - the job summary and the fork-count `::notice`;
  - both artifacts (`jacoco-coverage-reports`, `gradle-test-reports`).

  Also check that the Node 20 deprecation annotation for `checkout`/`upload-artifact` is gone.

### Claude's Discretion
- **DEBT-07 mechanism.** Research disagrees: STACK prefers a scoped `exclude` on `testRuntimeClasspath`; ARCHITECTURE prefers a test-scoped `dependencySubstitution` built from the catalog (`libs.lz4.java`), so no version is hard-coded.
  - Recommendation: the scoped exclude, placed next to the capability-rule block with a comment.
  - Pin it with a **new Docker-free test class** (not inside `Lz4RoundTripLiveTest`, which stays Docker-tagged). It asserts that `ClassLoader.getResources("net/jpountz/lz4/LZ4Factory.class")` yields exactly one URL, from `at.yawk.lz4`.
  - Verify with `dependencyInsight` on `testRuntimeClasspath`, `runtimeClasspath`, `integrationTestRuntimeClasspath` and `clickhouseDependencies`.
- **CI-14 task.** The task name (`printTestForks` in STACK, `reportTestForks` in ARCHITECTURE) and its exact output. Read lazily via `tasks.named<Test>("test").map { it.maxParallelForks }`. The step uses `./gradlew -q --console=plain <task> | tail -n 1`; `nproc` appears in the message as context only. Keep the summary heading and the `::notice` (CI-09). No copy of the formula stays in shell.
- **DEBT-08 row value.** One `@CsvSource` row in the existing D-01 NanoTimestamp block of `MetadataFreeJsonWriterTest` whose expected rendering is mantissa-with-fraction scientific notation. PITFALLS suggests `io.debezium.time.NanoTimestamp, 123, 1.23E-7`; confirm it against `BigDecimal.valueOf(v, 9).toString()`.
- **`tests.yaml` header comment.** Rewrite the header comment to describe the new state (fork-count source, pins, permissions).
- **FORK-DELTA rows.** Rewrite the CI row (line ~51). Close Known costs item 7 (999.2). Add a test-head deletion row, a Dependabot row, and the T-14-16 closure. Add a line stating that the other upstream workflows are intentionally untouched. Edit only Phase 17's rows; raw numbers are refreshed in Phase 20.
- **Plan split, commit order and evidence layout.** By precedent, use a measured approval bundle and a phase evidence file for: the SHA resolution table, the `javap` diff, the `dependencyInsight` outputs, the red flip and the run URL.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope and acceptance
- `.planning/ROADMAP.md` §"Phase 17: CI, Build & Test Hygiene" — success criteria 1-5, the shared-file note, the CI-17 push rules (D-13 precedent), the DEBT-06 research flag
- `.planning/REQUIREMENTS.md` — CI-11..CI-17, BLD-14, DEBT-06..DEBT-08; Future Requirements (other workflows, Gradle 9.8.0 / Lombok 1.18.48 not approved)
- `.planning/STATE.md` §Blockers/Concerns — [Env] `TESTCONTAINERS_HOST_OVERRIDE`, [Tooling] no `gh`, [CI] runner date, Node 20 deprecations, and the `test-head.yaml` finding

### Research (milestone-level, 2026-10-10)
- `.planning/research/STACK.md` — action versions and SHAs, `persist-credentials`, Lombok key semantics, the 999.2 mechanisms tried, the `printTestForks` prototype and the CI step replacement
- `.planning/research/ARCHITECTURE.md` §"Lz4RoundTripLiveTest response-leg assertion", §"999.2", §"CI hygiene" — the raw-HTTP fallback design, the `LZ4Factory.class` single-URL pin, the CI change table
- `.planning/research/PITFALLS.md` §"Pitfall 9" (Lombok `javap`), §"Pitfall 10" (SHA pinning: annotated tags, impostor commits, scope, the pin guard), the `NetworkSendBytes` measurements near line 21, IN-06 row near line 301
- `.planning/research/SUMMARY.md` — phase overview

### Findings being closed
- `.planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-REVIEW.md` — WR-01 (LZ4 response leg), WR-02 (fork-count formula copy)
- `.planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-SECURITY.md` — T-14-16 (action pinning / token permissions)
- `.planning/milestones/v1.7.0_gg1-sync-phases/15-suite-gate-publication-rollout/15-REVIEW.md` — IN-06 (NanoTimestamp edge pins)
- `.planning/phases/999.2-test-classpath-org-lz4-duplicate/` — backlog 999.2 detail
- `.planning/milestones/v1.7.0_gg1-sync-phases/14-dependency-bumps-ci/14-CONTEXT.md` — D-10..D-14 (CI-08 trigger, fork-count step shape, push and red-run precedent)

### Files edited
- `.github/workflows/tests.yaml` — runner, permissions, pins, `persist-credentials`, fork-count step, header comment
- `.github/workflows/test-head.yaml` — deleted (D-01)
- `.github/dependabot.yml` — github-actions entry (D-03, D-04)
- `build.gradle.kts` — fork-count task (CI-14), test-scoped lz4 fix (DEBT-07); capability rule at L60-72, `maxParallelForks` at L201-205
- `lombok.config` — the `jacksonVersion` line
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/Lz4RoundTripLiveTest.java` — response-leg assertion
- `src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonWriterTest.java` — DEBT-08 row (NanoTimestamp block around L322-330)
- `docs/FORK-DELTA.md` — CI row (L51), Known costs item 7 (999.2), new rows per D-02 and D-03
- `.planning/MERGE-CHECKLIST-v1.7.0.md` — test-head line (D-02)
- `.planning/scripts/fork-anchors.txt` — read-only unless an anchor moves (`tests.yaml timeout-minutes: 45` L40, `withCapability` L46)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Lz4RoundTripLiveTest` already builds its own client-v2 (`compressClientRequest(true)`, `compressServerResponse(true)`, `useHttpCompression(false)`, `serverSetting("network_compression_method","lz4")`, L49-66). It reads `system.query_log` for the insert's query id (`CompressedReadBufferBlocks`, L166-173). The response-leg check mirrors that query for the read's id.
- `Lz4RoundTripLiveTest.lz4FactoryLoadsFromTheMaintainedFork` checks `LZ4Factory`'s code source. It does not notice a second jar further down the path, hence the new single-URL test.
- `MetadataFreeJsonWriterTest` has a `@CsvSource` D-01 block with NanoTimestamp rows `0 → 0E-9`, `1 → 1E-9`, `-1 → -1E-9` (L322-330); DEBT-08 adds one row there.
- `.github/scripts/jacoco_summary.py` feeds the job summary; it is unchanged.

### Established Patterns
- `tests.yaml` (current) uses `ubuntu-latest`, `checkout@v4`, `setup-java@v6`, `gradle/actions/setup-gradle@v6` and `upload-artifact@v4` ×2. It has no `permissions` block. Its fork-count step repeats the `nproc/2` formula, including the `CLICKHOUSE_CLUSTER_NAME` branch. The trigger is branch pushes only (CI-08), with SHA-keyed concurrency.
- `ClickHouseFieldDescriptor` (`src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseFieldDescriptor.java`) is the only `@Jacksonized` class. The `javap` comparison targets it and its generated builder.
- `test` and `testUnit` both use `sourceSets["test"].runtimeClasspath`, so one test-scoped lz4 fix covers both.
- A `.planning/` edit by script slices to the target section first (the `c1480f8f` precedent).

### Integration Points
- The fork-count Gradle task is the step's first Gradle call; the compile step then reuses its daemon.
- The Docker-free tests (pin guard D-06, lz4 single-URL) must be picked up by `testUnit`, i.e. not tagged `clickhouse`.
- The workflow-file test reads a path relative to the project directory; Gradle's test working directory is the project root.

</code_context>

<specifics>
## Specific Ideas

- The user consistently chose the smallest durable option: delete rather than half-disable, config-only CI-15, a one-off red flip rather than a standing control, and a wide bound rather than a tight one. It was paired with cheap standing guards where they catch real regressions: the pin-guard test and the checklist line.
- The single CI-17 run proves everything in the phase, not just the workflow.

</specifics>

<deferred>
## Deferred Ideas

- Dependabot settings enablement on the fork: a manual check for the user, listed in Phase 20's operator steps (D-05). It is not a Phase 17 action.
- Upgrading and pinning the other upstream workflows (`release`, `pre-release`, `increment-version`, `integrationTests`). This is already a Future Requirement and is not discussed further.
- An invariant-script check that `test-head.yaml` stays absent. Considered and declined in favour of the checklist line (D-02).

</deferred>

---

*Phase: 17-ci-build-test-hygiene*
*Context gathered: 2026-10-11*
