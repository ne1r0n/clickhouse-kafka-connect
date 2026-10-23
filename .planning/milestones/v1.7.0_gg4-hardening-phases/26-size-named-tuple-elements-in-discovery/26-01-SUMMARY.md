---
phase: 26-size-named-tuple-elements-in-discovery
plan: "01"
subsystem: testing
tags: [clickhouse, describe-table, fixtures, spike, testcontainers, junit5, gradle-javaexec]

requires:
  - phase: 25-dependency-absorption-upstream-mini-sync
    provides: green sync/v1.7.0 tree, tail anchors gate and evidence-file shape
provides:
  - captureDescribeFixtures JavaExec task and the 47-fixture DescribeFixtureCatalog (one committed DDL list)
  - 95 committed fixture files holding the server's raw DESCRIBE rows and the production Column snapshot of every fixture
  - E-02 spike record (double row CONFIRMED, nine-step classifier rule, measured pre-fix affected set of 25)
  - live drift premise (DescribeFixtureDriftLiveTest) and Docker-free fixture guards (DescribeFixturesUnitTest)
affects: [26-02, 26-03, 26-04, 26-05, 28-jacoco-fork-report]

actuals:
  tokens: 32900
  tasks: 3
  commits: 2
plan_head_before: 4d4b91e9286cac55f9ff012bf7c54bc6e0657893
plan_head_after: 01f387a857d8bb845009d82c4ce2e619b2866588

tech-stack:
  added: []
  patterns:
    - "one catalog, one DESCRIBE reader, one Column-snapshot renderer shared by the capture harness, the drift premise and the live round-trip helpers"
    - "committed server-output fixtures with a SERVER_VERSION sidecar and a live drift test for a floating image tag"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java
    - src/test/resources/describe-fixtures/ (47 .jsonl, 47 .columns.txt, SERVER_VERSION.txt)
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md
  modified:
    - build.gradle.kts
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java

key-decisions:
  - "Spike facts come from committed server rows, not memory: the structural-then-declared double row is CONFIRMED on ClickHouse 26.9.19.9 (n02 prints c.size0 as UInt64, then as Array(Tuple(a UInt8)))"
  - "The measured pre-fix affected set equals the 25 predicted keys (s4_a01..s4_a13, n01..n04, x01..x07, r01); no look-alike, s4_o01, n05 or x08 logs the COLS-14 WARN, and no fixture logs any other WARN"
  - "NullableRoundTrip.describeRows now reads through DescribeFixtureCatalog.describeRawLines, so the harness, the drift premise and the live tests share one DESCRIBE reader"

patterns-established:
  - "Re-capture is one command: env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures, followed by a reviewed diff"

requirements-completed: [DESC-06]

coverage:
  - id: D1
    description: "captureDescribeFixtures re-captures the raw DESCRIBE rows and the production Column snapshot of 47 fixtures reproducibly (two runs byte-identical)"
    requirement: DESC-06
    verification:
      - kind: other
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures (run twice; diff -r silent; 47 DESCRIBE_FIXTURE lines)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Spike record E-02: double row CONFIRMED from the committed rows, classifier rule stated before any src/main change, measured affected set equals the WARN oracle"
    requirement: DESC-06
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java (95 tests) plus the E-02 verify script (SPIKE-RECORDED)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Live drift premise: every committed DESCRIBE line and Column snapshot equals the live server's, per fixture, with the key in the failure message"
    requirement: DESC-06
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java (94 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Shared DESCRIBE reader: NullableTupleMatrixLiveTest stays 33 tests with 0 failures through NullableRoundTrip.describeRows delegation"
    requirement: DESC-06
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java (33 tests, 0 failures)"
        status: pass
    human_judgment: false

duration: 14min
completed: 2026-10-21
status: complete
---

# Phase 26 Plan 01: Live DESCRIBE Spike, Capture Harness and Drift Premise Summary

**A one-command JavaExec harness captures the server's raw DESCRIBE rows and the production Column snapshot for 47 Tuple/Array/Map size-name fixtures on ClickHouse 26.9.19.9, confirms the structural-then-declared double row, states the nine-step classifier rule and measures the 25-table pre-fix affected set with the connector's own COLS-14 WARN, all before any src/main change.**

## Performance

- **Duration:** about 14 min (approximate; the start time was not recorded)
- **Started:** about 2026-10-21T20:44Z
- **Completed:** 2026-10-21T20:58Z
- **Tasks:** 3
- **Files modified:** 102 (95 fixture files, 4 new Java classes, NullableRoundTrip.java, build.gradle.kts, the evidence file)

## Accomplishments

- `captureDescribeFixtures` (JavaExec, group verification, wired into no other task) starts the suite's ClickHouse image, creates all 47 `DescribeFixtureCatalog.FIXTURES` tables and writes `<key>.jsonl`, `<key>.columns.txt` and `SERVER_VERSION.txt`; two runs on image `sha256:bdb5c911...` are byte-identical.
- E-02 records `double_row: CONFIRMED` from the committed n02 rows, the nine-step classifier rule under `classifier_rule: stated before any src/main change`, and `affected_prefix` = the 25 keys whose production `describeTable` logged the unpaired-element WARN (all 13 `s4_a` keys, none of the 19 look-alikes, not `s4_o01`); `other_warn_prefix: -`.
- `DescribeFixtureDriftLiveTest` (94 tests) keeps the committed rows honest against the floating `latest` image; `DescribeFixturesUnitTest` (95 tests, Docker-free, in `testUnit`) guards the file set, row parsing and parent-type parsing (A4).
- `NullableRoundTrip.describeRows` delegates to the shared `describeRawLines`; `NullableTupleMatrixLiveTest` stays at 33 tests, 0 failures. `testUnit`: 955 tests, 0 failed (baseline 860 plus 95).
- Tail gate at the new commits: `anchor_rows=72 ... result=PASS`.

## Task Commits

1. **Task 1: End-to-end capture (tracer)** - nothing committed by design; its files land in the Task 3 test commit (E-01 in the evidence file)
2. **Task 2: Spike record and fixture guards** - nothing committed by design; lands in the Task 3 test commit (E-02)
3. **Task 3: Drift premise, shared reader, commits, gate** - `40ffda7e` (test), `01f387a8` (docs, E-03)

**Plan metadata:** the docs(26-01) SUMMARY and state commit follows this file.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java` - the 47-fixture DDL list, shared raw DESCRIBE reader, Column-snapshot renderer, fixture resource reader
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCapture.java` - the capture harness `main()` (exit 0/1/2, metadata-only output)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixturesUnitTest.java` - Docker-free fixture-set, row-parse and parent-type-parse guards
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureDriftLiveTest.java` - live drift premise and production-describe snapshot check
- `src/test/resources/describe-fixtures/` - 47 `.jsonl`, 47 `.columns.txt`, `SERVER_VERSION.txt` (server 26.9.19.9)
- `build.gradle.kts` - the `captureDescribeFixtures` task
- `src/test/java/com/clickhouse/kafka/connect/sink/NullableRoundTrip.java` - `describeRows` delegates to the shared reader; two constants and five imports removed
- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md` - E-01..E-03 and the 12-entry index

## Decisions Made

- Used `chc.version()` for the server version in the harness and the drift line instead of a separate `SELECT version()` query helper (same result, no new code).
- The harness JVM printed `java=17.0.9`, which resolves flagged assumption A1 (the JavaExec runs on Java 17 without an explicit launcher); recorded in E-01.

## Deviations from Plan

None - plan executed exactly as written.

Two observations differ from the plan's prose but change no result, both recorded in E-02: `s4_u18` is not an empty-row fixture (it emits one size-named row, the structural `c.v.size`); only `s4_u16` has root rows alone, and `s4_u12`/`s4_u13` have no size-named sub-column row (the name is the root column). The plan's clean-tree command was applied with `.planning/STATE.md` also excluded, because the orchestrator left it modified before this plan started.

## TDD Gate Compliance

The plan is `type: execute`; its two `tdd="true"` tasks add guards over fixtures the Task 1 harness had already produced, so no failing-first commit exists or was planned (the plan commits once, in Task 3). The red-first discipline of this phase starts in plan 26-03.

## Issues Encountered

None. The OpenJ9 Mockito crash did not occur in the `testUnit` run (955 tests, 953 succeeded, 2 skipped, 0 failed).

## Known Stubs

None.

## Threat Flags

None. The harness writes only below the directory its Gradle task passes, refuses cloud and cluster environments, and prints metadata only (T-26-01, T-26-02 mitigated as planned).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 26-02 can extract `DescribeTableBuilder` and prove it behaviour-preserving against the pre-extraction `.columns.txt` snapshots committed here.
- Plan 26-03 reads `affected_prefix` (25 keys) from E-02 and implements the stated nine-step rule; its red set should equal that measured set plus any snapshot differences.
- Re-capture command if `latest` moves: `env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain captureDescribeFixtures`, then review the diff.

## Self-Check: PASSED

- Created files present: the four Java classes, `SERVER_VERSION.txt` and the evidence file (checked with `[ -f ]`).
- Commits `40ffda7e` and `01f387a8` are ancestors of HEAD.
- Verify gates printed: CAPTURE-TRACER-GREEN, PINGUARD-GREEN, SPIKE-RECORDED, DRIFT-GREEN, SPIKE-COMMITTED.

---
*Phase: 26-size-named-tuple-elements-in-discovery*
*Completed: 2026-10-21*
