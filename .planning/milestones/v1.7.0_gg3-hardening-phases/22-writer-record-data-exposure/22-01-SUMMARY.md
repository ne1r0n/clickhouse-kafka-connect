---
phase: 22-writer-record-data-exposure
plan: "01"
subsystem: testing
tags: [junit5, openj9, mockito-removal, clickhouse-helper-client, wrt-11]

requires:
  - phase: 18
    provides: "WRT-11 null-message pins and the OpenJ9 retransformClasses deferred item"
  - phase: 21
    provides: "D-14 spotless baseline method (throwaway git archive copy)"
provides:
  - "CountingHelperClient: Mockito-free ClickHouseHelperClient test double counting isUseClientV2/pinReplica/unpinReplica"
  - "ClickHouseWriterNullMessageUnitTest migrated off Mockito, all five WRT-11 cases and assertions kept"
  - "22-EVIDENCE.md with E-01 (D-16 spotless baseline, anchors mirror, HARN-08 before/after, F7 reading)"
affects: [22-02, 22-03, writer-tests]

actuals:
  tokens: 3420
  tasks: 2
  commits: 2
plan_head_before: 80cb10f477f847d9373a6c832fbe8b5a953403f7
plan_head_after: 25bcc98b603a6bbb20fdaeb76eb57bc3895270ab

tech-stack:
  added: []
  patterns:
    - "Hand-written ClickHouseHelperClient subclass closed per test with try-with-resources, no bytecode instrumentation"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java
    - .planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java

key-decisions:
  - "D-10 followed: a real ClickHouseHelperClient subclass with a dummy builder (127.0.0.1:8123) replaces the Mockito mock; no production change"
  - "F7 reading recorded in E-01: the phrase 'one level up' is met by the D-10 subclass, since doInsertRawBinary calls the helper client directly"

patterns-established:
  - "Mockito-to-exact-equality mapping: never() -> assertEquals(0, ...), times(1) -> assertEquals(1, ...)"

requirements-completed: [HARN-08]

coverage:
  - id: D1
    description: "ClickHouseWriterNullMessageUnitTest no longer uses Mockito; all five WRT-11 cases still pass against the real doInsertRawBinary catch via CountingHelperClient"
    requirement: HARN-08
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java (testUnit: tests=5 failures=0 errors=0 skipped=0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Full testUnit suite is green at the test(22-01) commit"
    verification:
      - kind: unit
        ref: "./gradlew testUnit --rerun (Tests summary: 577 tests, 575 succeeded, 0 failed, 2 skipped)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Whole-tree spotlessApply numstat unchanged against the D-16 baseline (46 files, md5 f2e32c7b...) and fork-anchors mirror rows=59 bad=0"
    verification:
      - kind: other
        ref: "/tmp/gsd22/exec/spotless-gate.sh -> SPOTLESS-BASELINE-EQUAL; /tmp/gsd22/exec/anchors-22-01.txt"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-10-19
status: complete
---

# Phase 22 Plan 01: Mockito-free HARN-08 Migration Summary

**ClickHouseWriterNullMessageUnitTest now drives the real doInsertRawBinary catch through a hand-written CountingHelperClient subclass instead of an inline Mockito mock, closing the Phase 18 OpenJ9 retransformClasses deferred item with no production change.**

## Performance

- **Duration:** ~12 min
- **Completed:** 2026-10-19
- **Tasks:** 2
- **Files modified:** 3 (1 created fixture, 1 edited test, 1 evidence file)

## Accomplishments
- Added `CountingHelperClient` (`public final class ... extends ClickHouseHelperClient`): real constructor with a dummy `127.0.0.1:8123` builder, overrides `isUseClientV2()`, `pinReplica()` and `unpinReplica()`, exposes `useClientV2Calls()`, `pinCalls()` and `unpinCalls()`. Reusable by plans 22-02 and 22-03.
- Migrated all five WRT-11 cases: `never().pinReplica()` became `assertEquals(0, chc.pinCalls())` (3 places), `times(1)` pair became `assertEquals(1, chc.pinCalls())` and `assertEquals(1, chc.unpinCalls())`. Every `@DisplayName`, the `@ParameterizedTest` name, method signature and `RETRY_TRIGGER_MESSAGE` line is byte-identical to `fefd8648`. Each test closes its fake with try-with-resources.
- Mockito lines in the class and fixture: 5 before, 0 after. Class XML `tests=5 failures=0 errors=0 skipped=0` before and after.
- Opened `22-EVIDENCE.md` with E-01: D-16 spotless baseline (46 files, md5 `f2e32c7bc069ecd155830f82d26285d6`), anchors mirror `rows=59 bad=0`, HARN-08 before/after, F7 reading of SC3, and the D-16 import probe for 22-03.

The Phase 18 deferred item (OpenJ9 `retransformClasses` crash risk in `ClickHouseWriterNullMessageUnitTest`) is closed by HARN-08. Other Mockito use in the suite stays out of scope. 22-02 and 22-03 should reuse `CountingHelperClient`.

## Task Commits

1. **Task 1: CountingHelperClient and WRT-11 migration (tracer)** - `84e773be` (test)
2. **Task 2: 22-EVIDENCE.md with E-01** - `25bcc98b` (docs)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan)

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/CountingHelperClient.java` - Mockito-free helper-client double with call counters
- `src/test/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriterNullMessageUnitTest.java` - WRT-11 pins on the real catch, now Mockito-free
- `.planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md` - Phase 22 evidence index and E-01

## Decisions Made
- Followed D-10 as locked: subclass the real helper client rather than override the writer one level up. F7 reading recorded for the verifier in E-01.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. No OpenJ9 crash occurred in any of the three Gradle runs (before, after, full testUnit), so no core dumps needed cleanup. The full testUnit reported 577 tests, 575 succeeded, 0 failed, 2 skipped.

## Verification Results

- HARN08-GREEN, TESTUNIT-GREEN, SPOTLESS-BASELINE-EQUAL, HARN08-COMMITTED and E01-RECORDED all printed.
- `git log --format=%s fefd8648..HEAD -- src` shows exactly one commit, `test(22-01)`.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or trust-boundary change; the fixture targets 127.0.0.1 and opens no connection.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `CountingHelperClient` is ready for plans 22-02 (WRT-14 pin) and 22-03 (WRT-13/WRT-15).
- D-16 baseline is on record in E-01 for all later Phase 22 code commits.

## Self-Check: PASSED

Created files exist, both commits (`84e773be`, `25bcc98b`) are ancestors of HEAD, and `git diff --quiet fefd8648 HEAD -- src/main` holds.

---
*Phase: 22-writer-record-data-exposure*
*Completed: 2026-10-19*
