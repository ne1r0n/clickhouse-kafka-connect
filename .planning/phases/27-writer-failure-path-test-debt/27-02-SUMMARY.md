---
phase: 27-writer-failure-path-test-debt
plan: "02"
subsystem: writer
tags: [kafka-connect, clickhouse, jackson, value-free-failures, retry-classification, wrt-17, wrt-19]

requires:
  - phase: 27-writer-failure-path-test-debt
    provides: plan 27-01 WRT-16 rows (Jackson-group retry classification, green on the unchanged writer) and E-01 phase base
provides:
  - ClickHouseWriter.passesThroughUnchanged excludes Jackson exceptions link by link, so a Jackson failure on the JSON-as-string route is withheld like any conversion failure while a timeout beneath it still passes through
  - JsonAsStringKeyResidualLiveTest flipped to key-absent (task status, DLQ stack-trace header, Error inserting records throwable), planning identifiers scrubbed
  - six Docker-free WRT-17 rows in WriterValueFreeFailureUnitTest (three value-free, three same-instance pass-through pins)
  - fork-anchors row, FORK-DELTA amendments (22-REVIEW WR-01 closed), CHANGELOG bullet, 27-BEHAVIOUR-LEDGER.md (L-52, L-53)
affects: [27-03, 27-04, 27-05, 29]

actuals:
  tokens: 15900
  tasks: 3
  commits: 2

plan_head_before: 6219d104b446436fd0b1fcb3fee09593af3bc174
plan_head_after: 25aea1222a4b2ba7f35c17530f08550745c5f018

tech-stack:
  added: []
  patterns:
    - "Red first, flip second: new and flipped assertions seen red on the unchanged writer (E-03), user sign-off for the one broadened predicate recorded verbatim (E-04), then writer fix, flip, docs and anchor in one commit with the full suite run on exactly its staged tree"
    - "Per-link cause-chain classification: a Jackson link never counts as the IOException, the loop keeps walking to its cause"

key-files:
  created:
    - .planning/phases/27-writer-failure-path-test-debt/27-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - CHANGELOG.md
    - .planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md

key-decisions:
  - "assertJacksonRouteReached accepts either a node of the Jackson class or a WithheldMessageException whose original class is that class (research F-1, route b); signed off by the user's answer flip-as-shown (E-04), seen red in E-03 predicate_red"
  - "A Jackson exception sharing its chain with a non-Jackson IOException still passes through unchanged (accepted residual, user-accepted at plan time); pinned by three same-instance rows and documented in FORK-DELTA Known costs item 6, the CHANGELOG bullet and L-52"
  - "A1 held (real InvalidDefinitionException drives both Jackson-route rows), so fallback A1-F was not applied"

requirements-completed: [WRT-17, WRT-19]

coverage:
  - id: D1
    description: "A Jackson failure on the JSON-as-string route, and one inside an Array(JSON) element, become a value-free DataException with no Map key in the chain, rendering or logs; a Jackson exception alone from the stream does the same"
    requirement: "WRT-17"
    verification:
      - kind: unit
        ref: "WriterValueFreeFailureUnitTest#WRT-17 value-free (3 rows)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A Jackson exception over a SocketTimeoutException passes through as the same instance and stays retriable; the two accepted-residual shapes are pinned as same-instance pass-throughs"
    requirement: "WRT-17"
    verification:
      - kind: unit
        ref: "WriterValueFreeFailureUnitTest#WRT-17 pass-through and accepted residual (3 rows); WriterFailureClassificationUnitTest JACKSON_* rows (216 cases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The Map key reaches none of the task status cause chain, the DLQ exception.stacktrace header and the Error inserting records throwable on a real ClickHouse (latest x V2), with errors.tolerance none and all; no row lands; the Jackson class is still asserted"
    requirement: "WRT-17"
    verification:
      - kind: integration
        ref: "JsonAsStringKeyResidualLiveTest (4 tests) against ClickHouse 26.9.19.9, client V2"
        status: pass
    human_judgment: false
  - id: D4
    description: "JsonAsStringKeyResidualLiveTest carries no planning identifier in its Javadoc, comments or display names"
    requirement: "WRT-19"
    verification:
      - kind: other
        ref: "grep -cE 'D-[0-9]+|Pitfall|RESEARCH' JsonAsStringKeyResidualLiveTest.java = 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "FORK-DELTA (doWriteColValue row, Known costs item 6, release history), the CHANGELOG bullet and ledger row L-52 describe the closed route and the accepted residual in terms the probe supports"
    requirement: "WRT-17"
    verification: []
    human_judgment: true
    rationale: "Whether the closure wording reads correctly to operators and matches the pinned shapes is a judgment call that no test asserts; the string-count checks only prove the phrases are present"

duration: 153min wall clock (includes the blocking-human checkpoint wait; Task 1 ran in the previous executor, Task 3 in this one)
completed: 2026-10-23
status: complete
---

# Phase 27 Plan 02: Jackson exclusion in the writer's pass-through Summary

**Per-link JacksonException exclusion in ClickHouseWriter.passesThroughUnchanged, so a Jackson failure on the JSON-as-string route is withheld like any conversion failure while a timeout beneath it is still retried; the live key pin flipped to key-absent on the user's sign-off**

## Performance

- **Duration:** 153 min wall clock, including the wait at the Task 2 checkpoint
- **Started:** 2026-10-22T21:43:00Z (approximate; the previous executor did not record it)
- **Completed:** 2026-10-23T00:16:00Z
- **Tasks:** 3 (Task 1 red-first, Task 2 blocking-human checkpoint, Task 3 fix)
- **Files modified:** 7 tracked files plus the new ledger

## Accomplishments

- `passesThroughUnchanged` now walks the cause chain with its identity-guarded set and counts a link only when it is an `IOException` and not a `JacksonException`. A Jackson link never counts and the loop keeps walking, so a Jackson exception over a `SocketTimeoutException` is still rethrown as the same instance (retriable), while a Jackson exception alone becomes the value-free `DataException` with a `WithheldMessageException` cause. The fully qualified `ConnectException` became an import (D-04) and the Javadoc lost its planning tags.
- The three red rows in `WriterValueFreeFailureUnitTest` (JSON-as-string route, Array(JSON) element, Jackson alone from the stream) were seen red on the unchanged writer, A1 held (the real `InvalidDefinitionException` carries the key), and the three pass-through rows stayed green before and after.
- `JsonAsStringKeyResidualLiveTest` now asserts the Map key absent from the task status rendering, the DLQ `exception.stacktrace` header and every `Error inserting records` throwable, and the rendering carries the `<Jackson class> (message withheld: may contain record data)` line. It still asserts the Jackson class and still asserts that no row landed. On the unchanged writer it was seen red on the two key assertions only.
- The user's `flip-as-shown` answer is recorded verbatim in E-04 as the sign-off for the widened `assertJacksonRouteReached` predicate, with the predicate's seen-red run cited from E-03.
- FORK-DELTA (doWriteColValue row, Known costs item 6, release history), the CHANGELOG gg4 Bug Fixes bullet, the fork-anchors row (`anchor_rows=80`) and the new behaviour ledger (L-52, L-53, rollout lines, out-of-scope note) moved with the fix. 22-REVIEW WR-01 is closed on the record.
- The full suite ran on exactly the commit's tree: 1655 tests, 0 failures, 0 errors, the 18 named skips, ClickHouse 26.9.19.9, client V2.

## Task Commits

1. **Task 1: Red first on the unchanged writer** - nothing committed by design; the Docker-free rows, the residual probe, the flipped live test and the predicate seen-red run are recorded in E-03 and landed with the Task 3 commit
2. **Task 2: Predicate sign-off checkpoint (blocking-human)** - answered by the user directly to the orchestrator ("flip-as-shown", 2026-10-23T00:06:49Z), recorded in E-04
3. **Task 3: The fix, documents, anchor, ledger, full suite, one commit** - `a41d5cb1` (fix)

**Plan metadata:** `25aea122` (docs: red runs, decision and fix gate evidence, ledger)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java` - per-link Jackson exclusion, two imports, reworded Javadoc
- `src/test/java/com/clickhouse/kafka/connect/sink/db/WriterValueFreeFailureUnitTest.java` - six WRT-17 rows (27 tests in the class)
- `src/test/java/com/clickhouse/kafka/connect/sink/JsonAsStringKeyResidualLiveTest.java` - the flipped, scrubbed live pin and the widened route-reached helper
- `.planning/scripts/fork-anchors.txt` - `!(t instanceof JacksonException)` row under a Phase 27 comment
- `docs/FORK-DELTA.md`, `CHANGELOG.md` - closure wording and the accepted residual
- `.planning/phases/27-writer-failure-path-test-debt/27-BEHAVIOUR-LEDGER.md` - L-52, L-53, rollout lines for Phase 29
- `.planning/phases/27-writer-failure-path-test-debt/27-COMMAND-EVIDENCE.md` - E-03, E-04, E-05

## Decisions Made

- The route-reached predicate was widened (route b) rather than split into a new helper, because after the fix the Jackson runtime class survives in none of the three route-reaching tests. The user signed the widening off (E-04).
- The accepted residual stays pinned, not changed: a Jackson exception that shares its chain with a non-Jackson `IOException` passes through with its message. The E-03 probe shows that on this route only a record value whose own getter throws such a chain builds it, and the user accepted it at plan time (D-02 not reopened, ROADMAP.md not edited).

## Deviations from Plan

### Auto-fixed Issues

**1. [Plan expectation error] Spotless numstat for ClickHouseWriter.java is `10<TAB>11`, not `9<TAB>12`**
- **Found during:** Task 3 step 10
- **Issue:** The plan expects `9<TAB>12`. That figure appears only when the two new imports are unused, because Spotless `removeUnusedImports` then deletes both. I reproduced `9<TAB>12` on the phase-base file with the two imports added and no usage. With the fix in place both imports are used, so Spotless keeps them and the `org.*` import block moves as one hunk.
- **Fix:** None to the code. The imports sit exactly where the plan places them. The 46 pre-existing violations are the same set, and ClickHouseWriter.java is the only line that differs from the base (`10<TAB>11` against `9<TAB>10`). The measurement and the reason are in E-05. The third automated check of Task 3's `<verify>` compares against `9<TAB>12`, so run as written it reports a difference on that one line.
- **Files modified:** none (evidence only)
- **Committed in:** `25aea122` (E-05)

**2. [Housekeeping] E-05 timestamp corrected**
- **Found during:** SUMMARY write
- **Issue:** E-05 first carried a timestamp later than the clock at the time of writing.
- **Fix:** Replaced with the clock time of the gate run's completion (committed with the SUMMARY).

**3. [Housekeeping] Plan commit ledger created after the fact**
- **Issue:** The continuation executor started after Task 1 and the ledger file `gsd-plan-head-before-27-02` did not exist when the fix commit was made.
- **Fix:** Created from the fix commit's parent `6219d104`, which is the HEAD before any 27-02 commit; `commits: 2` is measured from it.

---

**Total deviations:** 1 plan-expectation error, 2 housekeeping items
**Impact on plan:** None on behaviour or scope. The Spotless expectation was mismeasured at planning time; no code change followed from it.

## Issues Encountered

None in the fix itself. The full suite passed on the first run (no OpenJ9 crash).

## Known Stubs

None.

## Threat Flags

None. The accepted residual (T-27-05) is the plan's own register entry and is pinned and documented.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for 27-03. The phase-base anchors count moves to 80 rows here and to 82 after 27-03; the Docker-free test counts are 27 for `WriterValueFreeFailureUnitTest` and 1166 for testUnit.
- Phase 29 reads L-52 and the rollout lines in `27-BEHAVIOUR-LEDGER.md`. The `insertFormat=json` route (`writeRecordsJson`) is listed there as an unmeasured backlog candidate for the user.
- Plan 27-05's E-13 row (a) points at E-04.

## Self-Check: PASSED

- Created and modified files exist on disk (`27-BEHAVIOUR-LEDGER.md`, the writer, both tests, `fork-anchors.txt`, FORK-DELTA, CHANGELOG, `27-COMMAND-EVIDENCE.md`).
- `a41d5cb1` and `25aea122` are ancestors of HEAD; the fix commit's tree equals the tree the full suite ran on.
- Task 3 checks: `WRT17-CODE-DOCS`, `LIVE-FLIP-WRITTEN`, the suite check (96 XML, 1655 tests, exact class counts, 18 named skips, one server version), the tail gate (`anchor_rows=80`, `result=PASS`) and the working-tree anchors check (80 rows, none bad) all pass. The Spotless comparison differs from the plan's figure as described in Deviation 1.

---
*Phase: 27-writer-failure-path-test-debt*
*Completed: 2026-10-23*
