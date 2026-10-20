---
phase: 22-writer-record-data-exposure
plan: "02"
subsystem: testing
tags: [junit5, wrt-14, retry-classification, scripted-writer, handle-exception, openj9-safe]

requires:
  - phase: 22-01
    provides: "CountingHelperClient (Mockito-free helper-client double) and the D-16 spotless baseline"
provides:
  - "ScriptedWriteFailureWriter: Mockito-free ClickHouseWriter subclass whose RowBinary hooks run the real doWriteColValue on a scripted column, value and stream"
  - "WriterFailureClassificationUnitTest: Docker-free WRT-14 pin, 21 failure kinds x clientV2 x errorsTolerance x entry, plus the ignorePartitions route (184 testcases)"
  - "22-EVIDENCE.md E-02: pin_testcases=184, outcome table, shape inventory"
affects: [22-03, 22-04, writer-tests]

actuals:
  tokens: 8200
  tasks: 2
  commits: 3
plan_head_before: 6c5d1eb03f64d13b58d925407b02740ea3d1f8e5
plan_head_after: bb8b7954c599fda4212a7faf1b73e75290057790

tech-stack:
  added: []
  patterns:
    - "Outcome-only classification pin: drive the real put/doLogic/doInsert/doInsertRawBinary/handler path, assert Outcome plus two unchanged reach markers, never an exception's text or chain"
    - "Fixture installed into Processing.dbWriter by reflection (installInto), so the real ProxySinkTask runs with a real writer subclass"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/ScriptedWriteFailureWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java
  modified:
    - .planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md

key-decisions:
  - "The V2 client wrap is modeled in the fixture (RuntimeException wrapped once in com.clickhouse.client.api.ClickHouseException, IOException passed through), as measured in 22-RESEARCH D-13; plan 22-04's live test covers the real client"
  - "NESTED_DATA_EXCEPTION is pinned as RUNTIME_WRAP, not DATA_EXCEPTION: Processing.doInsert wraps every writer failure before the handler's DataException test, so DATA_EXCEPTION stays in the enum and no case observes it"

patterns-established:
  - "Each case builds a fresh ProxySinkTask, ScriptedWriteFailureWriter, CountingHelperClient, InMemoryDLQ and LogCapture, and stops the task in a finally block so topic MBeans do not leak across 184 cases"

requirements-completed: [WRT-14]

coverage:
  - id: D1
    description: "Handler outcome (retriable, runtime wrap, return) is pinned for all 21 failure kinds that reach doWriteColValue's generic catch, for both clients, both tolerances, both entries, green on the unchanged writer"
    requirement: WRT-14
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java#classificationOutcomeIsPinned (testUnit: tests=184 failures=0 errors=0 skipped=0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The ignorePartitionsWhenBatching route (second Processing.doInsert overload) classifies INVALID_UUID and SOCKET_TIMEOUT identically"
    requirement: WRT-14
    verification:
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java#classificationOutcomeIsPinnedOnTheIgnorePartitionsRoute"
        status: pass
    human_judgment: false
  - id: D3
    description: "src/main is untouched and the full testUnit suite, spotless baseline and fork-anchors mirror are unchanged"
    verification:
      - kind: other
        ref: "git diff --quiet fefd8648 HEAD -- src/main; ./gradlew testUnit --rerun (761 tests, 759 succeeded, 0 failed, 2 skipped); SPOTLESS-BASELINE-EQUAL; anchors rows=59 bad=0"
        status: pass
    human_judgment: false

duration: 8min
completed: 2026-10-19
status: complete
---

# Phase 22 Plan 02: WRT-14 Classification Pin Summary

**A Mockito-free ScriptedWriteFailureWriter drives 21 real failure kinds through ProxySinkTask, Processing, doInsertRawBinary, ChunkFlusher and Utils.handleException, pinning 184 outcome-only cases green on the unchanged writer.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-10-19T20:09Z
- **Completed:** 2026-10-19T20:17Z
- **Tasks:** 2
- **Files modified:** 3 (2 created test files, 1 evidence file)

## Accomplishments
- `ScriptedWriteFailureWriter` (`public final class ... extends ClickHouseWriter`): the real `doInsert`, `doInsertBatch` and `doInsertRawBinary` run; `doInsertRawBinaryV1/V2` call the real `doWriteColValue(column, stream, value, false)`. API: constructor `(ClickHouseSinkConfig, ClickHouseHelperClient, Column, Data, Supplier<OutputStream>)`, `V2_INSERT_FAILED_MESSAGE`, `failPrimitiveWith`, `failNestedColumnWith`, `hookCalls()`, `updateMappingCalls()`, `stubTable()`, `installInto(ProxySinkTask)`, nested `FailingOutputStream(IOException, int)`; overrides `doInsertRawBinaryV1`, `doInsertRawBinaryV2`, `updateMapping`, `getTable`, `doWritePrimitive`, `doWriteCol`. It was committed complete with the tracer, so plans 22-03 and 22-04 reuse it unmodified.
- `WriterFailureClassificationUnitTest`: 21 `FailureKind`s, enums `Outcome`, `Entry`, `RetryClass`; `classificationOutcomeIsPinned` (176 cases: 168 matrix plus 8 `SOCKET_EXCEPTION` with `retryOnSocketException=true`) and `classificationOutcomeIsPinnedOnTheIgnorePartitionsRoute` (8 cases). `pin_testcases` is **184**, the count plan 22-03 must see unchanged.
- Each case also asserts two reach markers (an ERROR `Error writing value of ` event naming the scripted outer column, and an ERROR `Error inserting records` event with a throwable), the single hook call, and the DLQ count (batch size on RETURNS, else 0). A provocation that misses the generic catch therefore fails rather than passes vacuously.
- The pin source has no `.getMessage()`, `.getCause()`, `getRootCause`, direct handler call or Mockito (grep count 0). The WRT-14 adjacency edges are pinned: read/write timeout text at index 0 is RETRIABLE while the same text after a prefix is not; a message-less `IOException` takes the non-retriable path with no NullPointerException.
- E-02 recorded in `22-EVIDENCE.md` with the 21-row outcome table and the shape inventory (A, B, ignorePartitions overload, modeled V2 wrap).

No recipe was adjusted from research. The nested IO byte budgets (Map 3, Array 1) were confirmed with a throwaway scratch probe (deleted, never committed): the Map writes 1, 1, 1 then the 4-byte Int32, the Array writes 1 then the 4-byte Int32.

## Task Commits

1. **Task 1: Tracer, one failure kind (INVALID_UUID) end to end** - `dbab8472` (test)
2. **Task 2: Expand to 21 kinds, socket retry and ignorePartitions route** - `0590d20c` (test)
3. **Task 2: E-02 evidence** - `bb8b7954` (docs)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan)

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/ScriptedWriteFailureWriter.java` - real-writer fixture with scripted column, value and stream
- `src/test/java/com/clickhouse/kafka/connect/sink/WriterFailureClassificationUnitTest.java` - the WRT-14 pin
- `.planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md` - E-02

## Decisions Made
- The V2 client wrap is modeled in the fixture, not real (per the plan's flagged assumption); plan 22-04's live V2 test exercises the real client.
- `NESTED_DATA_EXCEPTION` is RUNTIME_WRAP / RETURNS, not DATA_EXCEPTION, because `Processing.doInsert` wraps every writer failure before `Utils.handleException` tests `e instanceof DataException`. The `DATA_EXCEPTION` outcome stays in the enum as the plan specified; no case observes it. Plan 22-03 should expect the same.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. No OpenJ9 crash in any Gradle run (tracer, full class, full testUnit, scratch probe), so no core dumps needed cleanup. In a verification loop a zsh variable named `path` overwrote `PATH`, which broke one command's tool lookups; it was rerun from a bash script with a renamed variable, with no effect on any recorded result.

## Verification Results

- PIN-TRACER-GREEN, PIN-FULL-GREEN, TESTUNIT-GREEN, SPOTLESS-BASELINE-EQUAL and E02-RECORDED all printed.
- Full testUnit: 761 tests, 759 succeeded, 0 failed, 2 skipped (577 from E-01 plus 184).
- `git diff --quiet fefd8648 HEAD -- src/main` holds; fork-anchors mirror `rows=59 bad=0`.

## Known Stubs

None.

## Threat Flags

None. Test code only: no new network endpoint, auth path or trust-boundary change. The helper-client double targets 127.0.0.1 and opens no connection.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 22-03 (WRT-13 and WRT-15) can reuse `ScriptedWriteFailureWriter` and `CountingHelperClient` unmodified and must see `pin_testcases=184` unchanged and the pin class, the fixture and `CountingHelperClient` byte-identical to the last `test(22-02)` commit (`0590d20c`).

## Self-Check: PASSED

Created files exist, commits `dbab8472`, `0590d20c` and `bb8b7954` are ancestors of HEAD, and `git diff --quiet fefd8648 HEAD -- src/main` holds.

---
*Phase: 22-writer-record-data-exposure*
*Completed: 2026-10-19*
