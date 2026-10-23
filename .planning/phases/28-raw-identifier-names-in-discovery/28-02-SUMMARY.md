---
phase: 28-raw-identifier-names-in-discovery
plan: "02"
subsystem: testing
tags: [describe-json, name-matrix, spike, fixtures, junit5, testcontainers, client-v1-v2]

requires:
  - phase: 28-raw-identifier-names-in-discovery
    provides: plan 28-01 phase base (f3b1c487), the NAME-01 pin and the evidence file with E-01 and E-02
provides:
  - RawNameMatrix, the one catalog of twelve spiked characters with names and enum DDL builders, SQL and JSON escapers, server premises and the cell renderer
  - RawNameMatrixLiveTest, the live spike on latest (13 cases) that captures the server's DESCRIBE rows and renders the V2 and V1 current forms
  - 24 committed server DESCRIBE row files plus SERVER_VERSION.txt, with a Docker-free guard (13 cases) and a tagged drift premise (12 cases)
  - E-03 and E-04 in 28-COMMAND-EVIDENCE.md, the per-character, per-shape record of server text, current V2, current V1 and decoded forms
  - test-only preparation for plan 28-03 (assertDiscoveredUnderRawNames, BACKSLASH_N_ELEMENT_NAME, REAL_NEWLINE_ELEMENT_NAME)
affects: [28-03, 28-04, 28-05, 28-06, phase-29-rollout]

actuals:
  tokens: 24900
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Capture the server's DESCRIBE bytes with a live test, copy them unedited, guard them Docker-free and compare them live under a drift tag"
    - "Record current forms per client from the production describeTable, rendered as JSON strings so control and non-ASCII characters are unambiguous"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrix.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixDriftLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java
    - src/test/resources/raw-name-matrix/
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
    - .planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md

key-decisions:
  - "The V2 and V1 current forms are read from the production describeTable of two explicitly built clients, so the pre-fix V1 forms Phase 29 needs are measured, not inferred"
  - "The in-suite live test asserts only the server premises that hold before and after the fix; byte equality with the committed rows is the describe-drift tagged test"
  - "E-03 is committed on its own docs commit after the test commit, because the Task 2 verify requires a clean tracked tree"

requirements-completed: [NAME-02]

coverage:
  - id: D1
    description: "RawNameMatrixLiveTest drives all twelve characters through table creation, the server's DESCRIBE, the premises P1 to P3 and the production describeTable of client V2 and client V1; two runs write byte-identical rows"
    requirement: NAME-02
    verification:
      - kind: integration
        ref: "RawNameMatrixLiveTest 13 tests, 13 succeeded (run 1 and run 2); diff -r of the two captures silent"
        status: pass
    human_judgment: false
  - id: D2
    description: "The captured rows are committed as the server's bytes with a Docker-free guard of the file set and the premises, and an on-demand drift premise"
    requirement: NAME-02
    verification:
      - kind: unit
        ref: "RawNameMatrixFixturesUnitTest 13 tests, 13 succeeded; testUnit 1206 tests, 0 failed"
        status: pass
      - kind: integration
        ref: "describeFixtureDrift --tests '*RawNameMatrixDriftLiveTest' 12 tests, 12 succeeded"
        status: pass
    human_judgment: false
  - id: D3
    description: "E-04 records, for 12 entries and 6 shapes (72 rows), the server JSON text, the current V2 and V1 forms and the decoded form, with c1 to c4 passing and the WR-01 server literal matching the NAME-01 pin"
    requirement: NAME-02
    verification:
      - kind: other
        ref: "python3 -I spike_checks.py capture-run2: c1=pass c2=pass c3=pass c4=pass; Task 3 verify SPIKE-RECORDED"
        status: pass
    human_judgment: false
  - id: D4
    description: "Test-only preparation for plan 28-03 compiles and is unused here: assertDiscoveredUnderRawNames, the two raw-name constants and the reworded Javadoc of BACKSLASH_N_ELEMENT and REAL_NEWLINE_ELEMENT with unchanged values"
    verification:
      - kind: other
        ref: "Task 2 verify SPIKE-COMMITTED (grep counts of the constants and the declaration); testUnit 1206 tests"
        status: pass
    human_judgment: false

plan_head_before: 94f246e33291b7542293be02f675de0b12a9096e
plan_head_after: b37265277d78859c56803382589f5e12b309a5d4

duration: 10min
completed: 2026-10-23
status: complete
---

# Phase 28 Plan 02: NAME-02 name-matrix spike Summary

**Twelve-character live spike of the server's DESCRIBE JSON for root columns, Tuple, Array(Tuple), Nullable(Tuple) and Map-value Tuple elements and enum labels, with the server rows committed as fixtures and the current V2 and V1 forms recorded per character before any src/main change.**

## Performance

- **Duration:** about 10 min
- **Started:** 2026-10-23T09:38:30Z
- **Completed:** 2026-10-23T09:49:00Z
- **Tasks:** 3 (one tracer, two auto)
- **Files modified:** 32 in the plan range (4 new test classes, 2 edited test files, 25 resources, the evidence file)

## Accomplishments

- `RawNameMatrix` is the single catalog: the twelve entries in the planned order, DDL builders for a names table (root column `DEFAULT 99`, Tuple, Array(Tuple), Nullable(Tuple), Map(String, Tuple)) and an enum table, independent SQL and JSON escapers, the P1 to P3 server premises and the cell renderer.
- The live spike passes for all twelve entries on ClickHouse 26.9.19.9 (`latest`), no DDL was rejected, and the server's DESCRIBE JSON carries the raw identifier for every character. Two capture runs wrote byte-identical rows (and byte-identical cells).
- The 24 `.jsonl` files and `SERVER_VERSION.txt` are copied unedited from run 2; the Docker-free guard re-checks the file set and the premises on them, and `RawNameMatrixDriftLiveTest` (tag `describe-drift`) compares them live.
- E-04 lists 72 rows. Findings, from the table only: V2 doubles backslashes in names (bs, bsn, btbsn) and keeps backslash-n, t, r for lf, tab and cr; dq is UNMAPPED on V2; V1 leaves tab and cr names UNMAPPED, replaces slash and double quote with U+FFFD, and throws NumberFormatException for the apostrophe enum; non-zero WARN counts are names V2 for sl, bt and btbsn and names V1 for lf (4 each).
- The real server literal for the WR-01 shape (`btbsn`) equals the NAME-01 pin's input after a to x and b to y (`wr01_matches_pin: yes`).
- testUnit 1206 tests, 0 failed, 2 skipped; Spotless numstat equal to the 46-line base; tail gate `anchor_rows=83 result=PASS`; no `src/main` difference from the phase base.

## Task Commits

1. **Task 1: Tracer, ctl and bs end to end** - no commit of its own (edits land in the Task 2 test commit, as the plan directs)
2. **Task 2: all twelve entries, fixtures, guard, drift, preparation** - `f028c57e` (test, 31 files); `1a4e2706` (docs, E-03)
3. **Task 3: spike record E-04** - `b3726527` (docs)

**Plan metadata:** the `docs(28-02): complete ...` commit that carries this SUMMARY, STATE.md and ROADMAP.md.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrix.java` - the catalog, escapers, premises, cell renderer and the plan 28-03 discovery assertion
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixLiveTest.java` - live spike (12 entry cases and the server version)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixDriftLiveTest.java` - tagged drift premise (12 cases)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/RawNameMatrixFixturesUnitTest.java` - Docker-free guard (13 cases)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/DescribeFixtureCatalog.java` - `resourceLines(directory, fileName)` overload; the one-argument form delegates
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` - two raw-name constants, reworded Javadoc, values unchanged
- `src/test/resources/raw-name-matrix/` - 24 server DESCRIBE row files and `SERVER_VERSION.txt` (`version=26.9.19.9`)
- `.planning/phases/28-raw-identifier-names-in-discovery/28-COMMAND-EVIDENCE.md` - E-03 and E-04

## Decisions Made

- Cell forms come from the production `describeTable` of an explicitly built V2 client and V1 client, so the connector's own default (V1) is measured too.
- The in-suite test asserts only before/after-stable premises; formatting equality with the floating image is the drift test, kept out of `test`.
- E-03 got its own docs commit because the Task 2 verify requires a clean tracked tree, and Task 3's docs commit then carries E-04 alone.

## Deviations from Plan

None - plan executed exactly as written.

Environment note, not a deviation: the shell had no `gsd_run`, so the shim `/tmp/gsd28/exec/gsdrun.sh` from plan 28-01 was sourced.

## Issues Encountered

None. No OpenJ9 crash occurred in any Gradle run, and the first tracer run was green.

## Known Stubs

None.

## Threat Flags

None. The plan adds tests, resources and evidence only; no network endpoint, auth path or production file access changed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 28-03 can pin the decode against measured server bytes: the committed rows, the `assertDiscoveredUnderRawNames` assertion and the two raw-name fixture constants are in place and green on the current code.
- E-04 holds the measured doubled forms (c1) and legacy label keys (c2) that the pins of plans 28-04 and 28-05 rely on, and the V1 forms for the Phase 29 old/new name table.

---
*Phase: 28-raw-identifier-names-in-discovery*
*Completed: 2026-10-23*

## Self-Check: PASSED

Verified: the four new test classes and SERVER_VERSION.txt exist; commits f028c57e, 1a4e2706 and b3726527 are ancestors of HEAD; the Task 1, Task 2 and Task 3 automated verifies printed SPIKE-TRACER-GREEN, SPIKE-COMMITTED, SPIKE-GATED and SPIKE-RECORDED.
