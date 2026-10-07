---
phase: 08-orchestration-jmx-re-plumbing
plan: 01
subsystem: jmx
tags: [jmx, kafka-connect, mbean, javax.management, testing, kafka-connect-sink]

requires:
  - phase: 07-type-mapping-writer-helper-client-config
    provides: a compiling tree with testUnit green (300 tests) and the two temporary build.gradle.kts test-compile excludes this plan un-does one of
provides:
  - "connectorName threaded end-to-end: ClickHouseSinkTask.start() -> ProxySinkTask -> SinkTaskStatistics"
  - "SinkTaskStatistics.buildMBeanName(connectorName, taskId, version) - pure static formatter restoring the pre-merge carrier contract"
  - "Per-topic MBean name composed from getMBeanName() + ',topic=' + topic (D-08), not a second format string"
  - "MBeanNameUnitTest un-excluded from the test compile source set and re-targeted at the new carrier (6 pre-existing + 5 new Docker-free cases, 11 total)"
  - "Three charset-less InputStreamReader calls in ClickHouseTestHelpers.java pinned to UTF-8, clearing 4 known live-suite reds ahead of plan 08-02"
affects: [08-02, 08-03, 10-harness-and-live-suite-restoration]

actuals:
  tokens: 11306
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Static pure-function name builder (buildMBeanName) + instance convenience methods (getMBeanName/getTopicMBeanName) that close over the instance's own fields"
    - "Compose a per-topic identifier from the task-level identifier by string concatenation rather than a second format string, so the two cannot drift independently"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java
    - src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java
    - build.gradle.kts
    - src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/AvroDecimalLogicalTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/RowBinaryUnsupportedTypeGateTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/AvroNullableUnionVariantTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterColumnContextTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/processing/ProcessingTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/MetadataFreeJsonInsertUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/NestedNullableCompositeWriteTest.java

key-decisions:
  - "Followed D-01/D-03/D-06/D-08 exactly: connectorName is a SinkTaskStatistics constructor arg and field; buildMBeanName is a pure static; getTopicMBeanName composes getMBeanName() instead of duplicating the format string."
  - "Rule 3 (blocking-issue auto-fix): 16 test call sites across 8 files not enumerated in the plan's file list construct SinkTaskStatistics directly with the deleted 1-arg constructor. Updated all to the 2-arg form with an explicit null connector name - the same rationale the plan already applied to the 4 enumerated getMBeanName/getTopicMBeanName call sites (neither test file ever sets a name prop)."
  - "Task 2's tdd=\"true\" tests pin behavior already implemented and committed in Task 1 (the per-topic composition). A genuine RED phase was not attempted - see TDD Gate Compliance below."

patterns-established:
  - "Static pure-function name builder + instance convenience methods: any JMX/identifier naming class needing both a unit-testable formatting contract and ergonomic internal call sites should follow this shape."
  - "Compose related format strings from one another (getTopicMBeanName from getMBeanName) rather than parameterizing two independent String.format templates."

requirements-completed: [ORCH-02, ORCH-03]

coverage:
  - id: D1
    description: "A started sink task registers a JMX MBean whose ObjectName carries connector=<name> sourced from ClickHouseSinkTask.start()'s props.get(\"name\"), threaded through ProxySinkTask into SinkTaskStatistics"
    requirement: ORCH-02
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#connectorNamePresentRendersFullMBeanName"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#nullConnectorNameRendersLiteralConnectorNull"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#emptyConnectorNameRendersEmptyConnectorValue"
        status: pass
    human_judgment: false
  - id: D2
    description: "The per-topic MBean ObjectName carries the same connector=<name> segment, composed from the task-level name rather than duplicated"
    requirement: ORCH-03
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#topicNameWholeStringShapeMatchesComposedLiteral"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#topicNameIsComposedFromTaskLevelName"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#emptyTopicRendersPresentButEmptyTopicValue"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#nullConnectorNameTopicNameInheritsNullHandling"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java#nonAsciiConnectorAndTopicRenderVerbatimAsStrictPrefixExtension"
        status: pass
    human_judgment: false
  - id: D3
    description: "MBeanNameUnitTest un-excluded from the compiled test source set and green inside testUnit (the canary this phase exists to restore)"
    verification:
      - kind: unit
        ref: "build/test-results/testUnit/TEST-com.clickhouse.kafka.connect.sink.MBeanNameUnitTest.xml (11 tests, 0 failures, 0 errors, 0 skipped)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Three charset-less InputStreamReader calls in the shared ClickHouse test helper now decode UTF-8 explicitly"
    verification:
      - kind: other
        ref: "grep-verified: 3 InputStreamReader constructions, 3 carrying StandardCharsets.UTF_8; compileTestFixturesJava/compileTestJava green"
        status: pass
    human_judgment: true
    rationale: "The behavioural proof (the 4 previously-failing live tests actually turning green) is a live-suite run belonging to plan 08-02's gate (D-12), not this Docker-free plan - this plan's own gate is compile + source assertion only, per the plan's own <action> text."
  - id: D5
    description: "ORCH-01 (Processing/DeliveryStrategy layering already upstream, zero stale 4-arg Processing call sites) and ORCH-04/FREE-01 (Utils.java and Processing.java reviewed; UtilsTest/ProcessingTest confirmed on upstream's versions; FREE-01 already true) discharged by verification, not code, per D-10/D-11"
    verification:
      - kind: other
        ref: "grep -rn \"new Processing(\" src/ -> 16 call sites, all 5-arg (1 in ProxySinkTask.java:53, 15 in ProcessingTest.java); git diff v1.6.0 HEAD -- Processing.java -> empty"
        status: pass
    human_judgment: true
    rationale: "This is a documentation/record-keeping requirement (write down what was read and confirmed), not something an automated test proves on its own - the plan's own MERGE-CHECKLIST.md row flips for this evidence are explicitly reassigned to plan 08-03 by this plan's prohibitions list, so this SUMMARY carries the evidence forward instead."

duration: 21min
completed: 2026-09-19
status: complete
---

# Phase 8 Plan 1: JMX connector=<name> Re-plumb Summary

**Restored the JMX `connector=<name>` ObjectName segment (deleted by the v1.6.0 merge) by threading `connectorName` from `ClickHouseSinkTask.start()` through `ProxySinkTask` into a new `SinkTaskStatistics(connectorName, taskId)` constructor, with the per-topic name composed from the task-level name rather than a second format string, and put the `MBeanNameUnitTest` canary back into the compiled test suite.**

## Performance

- **Duration:** ~21 min
- **Started:** 2026-09-19T12:27:16Z (approximate - phase execution start recorded in STATE.md)
- **Completed:** 2026-09-19T12:48:11Z
- **Tasks:** 3
- **Files modified:** 15 (task commits) + evidence-gathering reads (no edits) for ORCH-01/ORCH-04/FREE-01

## Accomplishments

- `SinkTaskStatistics` now owns connector-aware JMX naming: a `private final String connectorName` field, a 2-arg constructor, a pure static `buildMBeanName(connectorName, taskId, version)`, and instance methods `getMBeanName()`/`getTopicMBeanName(topic)` — restoring the pre-merge carrier contract that upstream `v1.6.0` deleted with no compile error and no conflict marker.
- The per-topic ObjectName is `getMBeanName() + ",topic=" + topic` (D-08 composition) — the two names share a literal prefix and cannot drift independently in null handling, colon handling, or key order.
- `MBeanNameUnitTest` is back in the compiled test source set (its `build.gradle.kts` exclude deleted) and green: 6 pre-existing assertions re-targeted at the new carrier (zero literal changes) plus 5 new Docker-free cases pinning the per-topic composition, empty-topic, null-connector, and non-ASCII/strict-prefix behaviours — 11 tests total, 0 failures/errors/skipped.
- Three charset-less `InputStreamReader` calls in the shared ClickHouse test helper now decode UTF-8 explicitly, clearing the source-level cause of 4 known live-suite failures (`JsonDynamicRoundTripTest` TYPE-07 ×2, `SmtSinkLandingTest` HDRS-09/CONV-08) ahead of plan 08-02's live gate.
- ORCH-01 and ORCH-04/FREE-01 discharged by verification: `Processing.java` is a 0-line diff against `v1.6.0`, all 16 `new Processing(...)` call sites in the tree are 5-arg, `UtilsTest.java` is a 0-line diff against `v1.6.0`, and `Utils.isServerError()`/`handleException()` already classify both `ClickHouseException` (V1) and `ServerException` (V2) — FREE-01 was already true in the merged tree.

## Task Commits

Each task was committed atomically:

1. **Task 1: End-to-end JMX connector-name threading + canary restoration** - `34afc938` (feat)
2. **Task 2: Pin the per-topic name (5 new Docker-free cases)** - `75aa8576` (test)
3. **Task 3: UTF-8 the three charset-less stream readers** - `1927992f` (fix)

_Plan metadata commit follows this SUMMARY per the executor's standard close-out; not yet created at the time this SUMMARY was written._

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/util/jmx/SinkTaskStatistics.java` - `connectorName` field/ctor arg, `buildMBeanName` pure static, `getMBeanName()`/`getTopicMBeanName(topic)` instance methods, internal callers updated
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` - 3-arg constructor, `new SinkTaskStatistics(connectorName, id)`
- `src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTask.java` - `connectorName = props.get("name")`, passed to `new ProxySinkTask(...)`
- `build.gradle.kts` - deleted the `MBeanNameUnitTest.java` test-compile exclude and its comment; reworded the block comment from plural to singular (one remaining file, Phase 10's)
- `src/test/java/com/clickhouse/kafka/connect/sink/MBeanNameUnitTest.java` - mechanical carrier swap (commit 1) + 5 new per-topic cases (commit 2)
- `src/test/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkTaskTest.java` - 3 call sites updated to `SinkTaskStatistics.buildMBeanName(null, taskId, Version.ARTIFACT_VERSION)` (+ `,topic=` composition for the topic-name case)
- `src/test/java/com/clickhouse/kafka/connect/sink/dlq/FailureTest.java` - 1 call site updated, `Version` import added
- `src/testFixtures/java/com/clickhouse/kafka/connect/sink/helper/ClickHouseTestHelpers.java` - `StandardCharsets.UTF_8` on all 3 `InputStreamReader` constructions
- 8 additional test files (`AvroDecimalLogicalTest`, `RowBinaryUnsupportedTypeGateTest`, `ClickHouseWriterTest`, `AvroNullableUnionVariantTest`, `ClickHouseWriterColumnContextTest`, `ProcessingTest`, `MetadataFreeJsonInsertUnitTest`, `NestedNullableCompositeWriteTest`) - 16 `new SinkTaskStatistics(0)` call sites updated to `new SinkTaskStatistics(null, 0)` (Rule 3 fix, see Deviations)

## Decisions Made

- Followed the plan's locked decisions (D-01/D-03/D-04/D-06/D-08/D-09) exactly: no deviation from the carrier shape, the version source, the composition rule, or the "zero literal changes" constraint on the 6 pre-existing `MBeanNameUnitTest` assertions.
- Used Option A from RESEARCH.md's Pattern 2 (call the pure static directly with an explicit `null` at the 4 upstream test call sites) rather than adding a convenience static overload — leaner diff, zero new public API surface, consistent with the milestone's fork-delta-shrinking goal.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] 16 additional test call sites construct `SinkTaskStatistics` with the deleted 1-arg constructor**
- **Found during:** Task 1 (`compileTestJava` failed after the constructor signature changed)
- **Issue:** The plan's "exactly four upstream test call sites" fact covered only the two connector-blind *static methods* (`getMBeanName(int)`/`getTopicMBeanName(int,String)`). It did not account for 16 direct *constructor* call sites (`new SinkTaskStatistics(0)`) across 8 test files not in the plan's `<files>` list: `AvroDecimalLogicalTest.java`, `RowBinaryUnsupportedTypeGateTest.java` (×4), `ClickHouseWriterTest.java` (×4), `AvroNullableUnionVariantTest.java`, `ClickHouseWriterColumnContextTest.java`, `ProcessingTest.java`, `MetadataFreeJsonInsertUnitTest.java`, `NestedNullableCompositeWriteTest.java` (×3).
- **Fix:** Updated all 16 call sites to `new SinkTaskStatistics(null, 0)` — the same `null`-connector rationale the plan already applied to the 4 enumerated static-method call sites, since none of these tests set a connector name either.
- **Files modified:** the 8 files listed above.
- **Verification:** `compileTestJava`/`compileTestFixturesJava` green; `testUnit` green (315 tests, 0 failures).
- **Committed in:** `34afc938` (Task 1 commit — same commit as the constructor change, since the tree does not compile without both together).

**2. [Rule 1 - stale plan-script assumption, not a code bug] Two automated verify-script patterns in the plan do not match the tree**
- **Found during:** Task 1's second verify block (`git diff v1.6.0 HEAD -- .../MBeanServerUtils.java` expected empty) and Task 3's first verify block (`grep -o 'new InputStreamReader([^)]*StandardCharsets\.UTF_8'` expected 3 matches)
- **Issue A:** `MBeanServerUtils.java` carries a pre-existing 11-line import-reorder diff against `v1.6.0` from commit `a1b46e48` ("sort and delete unused imports"), predating this plan's session entirely (confirmed via `git log` and an empty `git diff`/`git status` on the working tree for that file). The plan's verify script assumed this file was byte-identical to `v1.6.0`, matching the "Verified facts" table's claim — that claim was accurate at research time but the fact drifted before this plan executed, the same class of drift already documented for `Utils.java`'s 29-line import reorder.
- **Fix A:** None needed — the file was never edited by this plan (`git status --short` shows zero changes to it throughout). Documented here as evidence the constraint ("MUST NOT edit MBeanServerUtils.java") was honored; the verify script's specific automated assertion is what's stale, not the implementation.
- **Issue B:** The plan's `grep -o 'new InputStreamReader([^)]*StandardCharsets\.UTF_8'` pattern cannot match the plan's own recommended fix shape (`new InputStreamReader(queryResponse.getInputStream(), StandardCharsets.UTF_8)`) because `[^)]*` cannot span the closing paren of the nested `getInputStream()` call — confirmed by testing the exact pattern against the exact recommended replacement text in isolation (`grep` exits 1).
- **Fix B:** Verified the actual acceptance criterion ("3 readers, 3 carrying `StandardCharsets.UTF_8` as an argument") with a corrected pattern (`new InputStreamReader\([^;]*StandardCharsets\.UTF_8\)`, bounded by the statement terminator instead of parenthesis-exclusion), which confirms `readers=3 guarded=3`.
- **Verification:** `compileTestFixturesJava`/`compileTestJava` green; diff scoped to 4 hunks (import + 3 reader sites), no method signature touched.
- **Committed in:** N/A (no code change — plan-script defect only, worth fixing in the plan file itself in a future revision, not in this repo's source).

**3. [Process] Corrected a pre-commit staging error before it reached history**
- **Found during:** Task 1's commit step
- **Issue:** The first `git commit -m "..."` (without a trailing pathspec) committed the entire index, which included two pre-existing staged `.planning/` files (`milestone.lock`, `08-DISCUSS-CHECKPOINT.json`) that the dispatch prompt explicitly said to leave untouched.
- **Fix:** Used `git reset --soft HEAD^` (non-destructive — moves only the branch pointer, index and working tree unchanged) to undo the commit, then re-committed with an explicit trailing pathspec limited to this task's 15 intended files. Verified via `git status --short` that the pre-existing `.planning/` staged/unstaged state was restored byte-for-byte to its pre-commit shape.
- **Verification:** `git show --stat HEAD` after the fix shows exactly the 15 intended files; `.planning/` state unchanged.
- **Committed in:** `34afc938` (the corrected commit).

---

**Total deviations:** 3 (1 Rule-3 auto-fix, 1 stale-plan-script documentation, 1 process self-correction). **Impact:** The Rule-3 fix was necessary for the tree to compile at all and applies the plan's own stated rationale mechanically to sites the plan's fact-gathering missed; no scope creep beyond restoring compilability. The stale-script findings changed no code and are recorded so a future planner does not re-litigate a false "MBeanServerUtils was edited" or "UTF-8 fix incomplete" alarm. The process self-correction left zero trace in the final commit history.

## TDD Gate Compliance

Task 2 carries `tdd="true"`, but its five new test methods pin per-topic MBean naming behavior (`getTopicMBeanName`/composition) that was already implemented and committed in Task 1 (`34afc938`) — the plan's own design (D-09: "landed in a second commit **after** D-04's mechanical carrier swap"). Because the implementation under test was correct and already merged before these tests were written, there is no code state in which they could genuinely RED without deliberately regressing already-verified, already-committed production code (which the plan's prohibitions forbid: "MUST NOT... comment out... the null-version and colon-sanitization branches", and no prohibition permits temporarily breaking `getTopicMBeanName`'s composition either).

- **RED:** Not attempted in the classic sense. All 5 new tests passed on first run (11 tests total, 0 failures) immediately after being written, because they characterize behavior Task 1 already implemented correctly.
- **GREEN:** N/A — no implementation commit followed, because no implementation change was needed.
- **REFACTOR:** N/A — no implementation to refactor.
- **Commit shape actually used:** a single `test(08-01): ...` commit (`75aa8576`), matching the plan's own explicit instruction ("Append five `@Test` methods to `MBeanNameUnitTest`, landing as a **second commit**") rather than the canonical `test -> feat -> refactor` triad.

This is flagged per the TDD reference's "Missing RED commit... Flag in SUMMARY.md" fail-fast rule. The divergence is structural to this task's design (pinning tests for already-shipped behavior, explicitly sequenced as commit 2 of 2 behind the implementation in commit 1), not an execution shortcut — forcing a synthetic RED would have required violating the plan's own prohibitions against regressing the composition logic. No further action recommended.

## Issues Encountered

- The Edit tool's parameter decoding interpreted a literal `п`-style Java Unicode escape sequence in a `new_string` argument as an actual JSON Unicode escape, writing real Cyrillic/CJK characters into the source file instead of the literal 6-character escape sequences the plan required (to keep the `.java` file pure ASCII under the test JVM's `windows-1252` default). Detected via a post-edit `LC_ALL=C grep -c '[^ -~<tab>]'` check (plan's own acceptance criterion). Fixed with a direct `sed` byte-level edit after confirming the exact on-disk bytes with `od -c`, restoring single-backslash escapes. Verified zero non-ASCII bytes remain.
- `./gradlew spotlessApply -PspotlessFiles=...` does not scope Spotless to specific files in this project's plugin configuration (the property is not honored) — it reformats the whole tree, surfacing the pre-existing 52-file reformat backlog documented in STATE.md. Worked around each time by running the tree-wide `spotlessApply`, diffing to confirm only the intended files' formatting was needed, then `git checkout --` on every unrelated file to keep each commit scoped to its task (D-15). Confirmed idempotent by re-running `spotlessApply` a second time per task and observing zero additional changes to the intended files.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 08-02's live-suite gate (D-12) can now run against a clean baseline for this plan's scope: the JMX `connector=` segment exists (clearing `MBeanRegistrationLiveTest` JMX-04 ×2's NPE), and the 4 UTF-8 mojibake failures are cleared in source.
- **For plan 08-03:** the three commit hashes are `34afc938` (JMX re-plumb), `75aa8576` (per-topic test cases), `1927992f` (UTF-8 fixture fix). `.planning/MERGE-CHECKLIST.md` was **not** touched by this plan (per this plan's own prohibitions list, which reassigns all checklist bookkeeping to plan 08-03) — plan 08-03 owns both the D-07 Recorded Fact (old ObjectName string -> new ObjectName string, both segments, below) and the annotate-don't-flip notes for the `ClickHouseSinkTaskTest.java` row (commit `34afc938`) and the UTF-8/HARN-04 row (commit `1927992f`).
- **Rendered ObjectName strings, old -> new (for the D-07 Recorded Fact):**
  - Task-level, before this plan: `com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask<id>,version=v1.6.0_gg1`
  - Task-level, after this plan: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=v1.6.0_gg1`
  - Per-topic, before this plan: `com.clickhouse:type=ClickHouseKafkaConnector,name=SinkTask<id>,version=v1.6.0_gg1,topic=<topic>`
  - Per-topic, after this plan: `com.clickhouse:type=ClickHouseKafkaConnector,connector=<name>,name=SinkTask<id>,version=v1.6.0_gg1,topic=<topic>`
  - (Note: the `version=` segment was already `v1.6.0_gg1`, sourced from `Version.ARTIFACT_VERSION`, in the post-merge pre-Phase-8 tree — that source change happened at the Phase 6 merge, not in this plan. This plan's only rendered change is the new `connector=<name>,` segment.)
- **For Phase 10 / HARN-02:** the per-topic MBean names this plan produces (`getMBeanName() + ",topic=" + topic`, now carrying `connector=<name>`) are the exact names the `ClickHouseBase` `@AfterEach` MBean leak sweep will need to match once restored. Also carried forward: once a live JMX test case calls `put()`, `MBeanRegistrationLiveTest.registeredFor()`'s filter on the `connector` key alone will return both the task-level and every topic-level MBean under that connector — any new live case that inserts records must additionally filter or count by the `topic` key.
- No blockers for plan 08-02.

## Self-Check: PASSED

- All 8 modified source/build files confirmed present on disk.
- All 3 task commit hashes (`34afc938`, `75aa8576`, `1927992f`) confirmed in git log.
- All plan-level `<verification>` items re-run: full compile green, `testUnit` green (11/11 on the canary, 315/315 overall), exactly 3 commits since the pre-plan HEAD, `Processing.java`/`Utils.java` diffs confirmed as documented, zero `.planning/` files touched by this plan's commits, zero conflict markers.
- All task-level `<acceptance_criteria>` re-verified via their grep/test commands (with two documented plan-script pattern corrections, see Deviations #2).

---
*Phase: 08-orchestration-jmx-re-plumbing*
*Completed: 2026-09-19*
