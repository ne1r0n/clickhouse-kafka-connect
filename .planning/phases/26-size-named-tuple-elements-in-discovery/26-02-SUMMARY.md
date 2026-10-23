---
phase: 26-size-named-tuple-elements-in-discovery
plan: "02"
subsystem: database
tags: [clickhouse, describe-table, refactor, tuple, characterization-test, fork-anchors, junit5]

requires:
  - phase: 26-size-named-tuple-elements-in-discovery
    provides: "47 committed DESCRIBE fixtures with pre-extraction Column snapshots (plan 26-01)"
provides:
  - DescribeTableBuilder, the one fork-owned per-row DESCRIBE loop shared by client V1 and V2
  - DeclaredNullability.elementKey, the one public definition of a Tuple element's key
  - Docker-free characterization of the builder over all 47 fixtures (DescribeFixtureSnapshotUnitTest)
  - the 999.4 pin driven through the production loop instead of a test-local copy (23-REVIEW WR-02 closed)
  - Phase 26 anchor rows (DESC-07, DESC-08) and moved HLP-02 / COLS-10 counts
affects: [26-03, 26-04, 26-05, 28-jacoco-fork-report]

actuals:
  tokens: 8560
  tasks: 3
  commits: 2
plan_head_before: ca872441062a1f2aa901ac49fd09caf07db396e8
plan_head_after: 7083e993e1b500a32814571973e10838ad89c326

tech-stack:
  added: []
  patterns:
    - "one raw JSON line per addRow call, so a single entry point changes how a DESCRIBE row becomes a Column"
    - "behaviour-preserving extraction proven by committed pre-extraction snapshots, never by updating a snapshot"

key-files:
  created:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - .planning/scripts/fork-anchors.txt
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - .planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md

key-decisions:
  - "DescribeTableBuilder logs through LoggerFactory.getLogger(ClickHouseHelperClient.class) so operators' log configuration and LogCapture attachments keep matching"
  - "addRow declares throws JsonProcessingException and wraps nothing, so V1 and V2 keep their exact catch blocks"
  - "A snapshot or suite difference would have been a stop, not an expectation update; none occurred"

patterns-established:
  - "Docker-free unit pins feed raw JSONEachRow lines (DescribeFixtureCatalog.describeLine) so they exercise fromJsonRow and the production loop"

requirements-completed: [DESC-07]

coverage:
  - id: D1
    description: "describeTableV1 and describeTableV2 share one fork-owned DescribeTableBuilder; behaviour is unchanged on all 47 committed fixtures"
    requirement: DESC-07
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java (47 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The 999.4 pin builds its Table through DescribeTableBuilder and still asserts the defect (size-named element not a Column, one COLS-14 WARN, apply() true)"
    requirement: DESC-07
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java#sizeNamedTupleElementDroppedByDiscoveryIsNamedInOneWarning (3 rows)"
        status: pass
    human_judgment: false
  - id: D3
    description: "DeclaredNullability.elementKey is the one public Tuple element key; named, two unnamed and a backtick-named element are pinned"
    requirement: DESC-07
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java#elementKeyIsTheNameOrTheOneBasedPosition (4 rows)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The extraction commit is green on the full suite against ClickHouse latest with client V2 (1543 tests, 0 failed, 18 named skips) and the tail anchors gate passes with 74 rows"
    requirement: DESC-07
    verification:
      - kind: integration
        ref: "env -u TESTCONTAINERS_HOST_OVERRIDE ./gradlew --console=plain test --rerun; verify-merge-invariants.sh anchor_rows=74 result=PASS"
        status: pass
    human_judgment: false
  - id: D5
    description: "Client V1 yields the same JSONEachRow text as V2 over all 47 fixtures (flagged assumption A6)"
    requirement: DESC-07
    verification: []
    human_judgment: true
    rationale: "The full suite exercises V1 only through TableTest.extractCommentV1 and the V1 rows of ClickHouseWriterTest; plan 26-04 records the manual CLIENT_VERSION=V1 run of the drift class"

duration: 11min
completed: 2026-10-21
status: complete
---

# Phase 26 Plan 02: DescribeTableBuilder Extraction Summary

**The duplicated V1/V2 DESCRIBE row loop is now one fork-owned DescribeTableBuilder fed raw JSON lines, with a public DeclaredNullability.elementKey, proven behaviour-preserving by all 47 pre-extraction Column snapshots and a 1543-test live suite on ClickHouse 26.9.19.9.**

## Performance

- **Duration:** about 11 min (approximate; the start time was not recorded)
- **Started:** about 2026-10-21T21:00Z
- **Completed:** 2026-10-21T21:12Z
- **Tasks:** 3
- **Files modified:** 9 (2 new Java classes, 6 edited, the evidence file)

## Accomplishments

- `DescribeTableBuilder` holds the loop body moved verbatim: `numColumns` counted before any skip, the alias/materialized/ephemeral skip, the parent-skipped skip, the structural skip through `shouldSkipDescribeRow`, `declaredTypes`, `hasDefaults`, `Column.extractColumn`, and `false` only for an unmappable column after the same ERROR log. `describeTableV1` and `describeTableV2` keep their `.inner` early return, query, reader, try-with-resources and catch block; each shrank to one builder, one feed loop and `return builder.build()`.
- `DeclaredNullability.elementKey(ClickHouseColumn, int)` is the single definition of a Tuple element's key (name, or one-based position when unnamed); `collectTuple` calls it, `normalise` and `fromJsonRow` are untouched.
- `DescribeFixtureSnapshotUnitTest` runs the builder over the 47 committed `.jsonl` files and matched every `.columns.txt` on the first run, with no snapshot edited.
- The 999.4 pin now builds through the builder from `DescribeFixtureCatalog.describeLine` JSON lines and still asserts the defect under its unchanged name and display name; the loop-copy helper `discoverAsDescribeTableV2` is deleted (WR-02).
- Anchors moved in the extraction commit: HLP-02 `shouldSkipDescribeRow` 3 to 1, COLS-10 `buildDescribedTable(` 3 to 1, new DESC-07 `new DescribeTableBuilder(` 2 and DESC-08 `elementKey(` 2; the tail gate prints `anchor_rows=74 result=PASS`.

## Task Commits

1. **Task 1: DescribeTableBuilder, shrunken describe methods, elementKey, moved anchors** - nothing committed by design; lands in the Task 3 commit
2. **Task 2: pins drive the builder, 47-fixture snapshot test, elementKey rows** - nothing committed by design; lands in the Task 3 commit
3. **Task 3: extraction commit, full suite, spotless, tail gate (E-04)** - `9c186046` (refactor), `7083e993` (docs, E-04)

**Plan metadata:** the docs(26-02) SUMMARY and state commit follows this file.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeTableBuilder.java` - the per-row DESCRIBE builder (`addRow`, `build`)
- `src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java` - both describe methods delegate to the builder
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` - `elementKey`
- `.planning/scripts/fork-anchors.txt` - moved HLP-02 / COLS-10 counts and the Phase 26 rows (72 to 74 rows)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java` - `describeLine`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClientForkUnitTest.java` - `discoverWithBuilder`, 999.4 pin re-pointed, loop copy deleted
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureSnapshotUnitTest.java` - "DESC-07 snapshot: {0}" over 47 keys
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - "DESC-08 elementKey: {0}" with four rows
- `.planning/phases/26-size-named-tuple-elements-in-discovery/26-COMMAND-EVIDENCE.md` - E-04

## Decisions Made

- The builder keeps the `ClickHouseHelperClient` logger name and the four message texts verbatim (T-26-04), so no new text or logger appears.
- `addRow` throws `JsonProcessingException` unwrapped, so the V1 `catch (ClickHouseException | JsonProcessingException)` and the V2 `catch (Exception)` behave as before.
- The `elementKey` Javadoc names the three consumers (`collectTuple`, plan 26-03's classifier, Phase 28's raw-name pairing).

## Deviations from Plan

None - plan executed exactly as written.

Observation, no deviation: after `spotlessApply` in a detached HEAD worktree (47 files rewritten, which shows it ran), six of the seven Java files are byte-identical. `ClickHouseHelperClient.java` differs only in the order of the `lombok`/`slf4j` imports, a baseline rewrite that predates this plan (`HEAD~1` has the same import block, which the plan did not touch). Recorded in E-04.

## Issues Encountered

None. The OpenJ9 Mockito crash did not occur: `testUnit` 1006 tests, 1004 succeeded, 0 failed, 2 skipped; the full suite ran once.

## Known Stubs

None.

## Threat Flags

None. The four moved log messages carry column names only (T-26-04), and the snapshot test plus the live suite on the committed extraction tree discharge T-26-03.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 26-03 can write its red-first classifier test against the unchanged-behaviour tree: `DescribeTableBuilder.addRow` is the single entry point to change, and `DeclaredNullability.elementKey` is public.
- Its red set should equal the 25 affected keys measured in E-02; the 999.4 pin keeps its name in this plan and flips only in 26-03.
- Plan 26-04 owns the manual V1 run of the drift class (flagged assumption A6).

## Self-Check: PASSED

- Created files present: `DescribeTableBuilder.java`, `DescribeFixtureSnapshotUnitTest.java` and this evidence (checked with `[ -f ]`).
- Commits `9c186046` and `7083e993` are ancestors of HEAD; `git rev-list --count ca872441..HEAD` is 2.
- Verify gates printed: EXTRACTION-WIRED, PINS-ON-BUILDER, EXTRACTION-SUITE-GREEN, EXTRACTION-GATED.

---
*Phase: 26-size-named-tuple-elements-in-discovery*
*Completed: 2026-10-21*
