---
phase: 22-writer-record-data-exposure
plan: "04"
subsystem: testing
tags: [wrt-13, d-12, d-13, d-15, live-test, dlq-headers, record-data-exposure, behaviour-ledger]

requires:
  - phase: 22-03
    provides: "The value-free DataException and WithheldMessageException from the writer's generic catch, ThrowableTexts, and ledger rows L-42..L-47"
  - phase: 22-01
    provides: "CountingHelperClient and the harness the Docker-free DLQ test reuses"
  - phase: 22-02
    provides: "ScriptedWriteFailureWriter, reused unmodified by ValueFreeDlqHeadersUnitTest"
provides:
  - "WriterErrorPathLiveTest: WRT-13 live put() pins on ClickHouse latest x client V2 for an invalid UUID and an unparseable Int128, under errors.tolerance=none and errors.tolerance=all"
  - "InMemoryDLQ.exceptions() and connectHeaders(Throwable): Connect 4.1.1 exception-header rendering, additive"
  - "ValueFreeDlqHeadersUnitTest: Docker-free D-15 header pins through the real ProxySinkTask on V1 and V2"
  - "Ledger row L-48 and live pins on L-42 and L-43; evidence entry E-04 with the D-13 trace"
affects: [22-05, phase-24-release]

actuals:
  tokens: 5800
  tasks: 2
  commits: 3
plan_head_before: 52abf15488ceadbbd29fb73f578674dc1cc2704d
plan_head_after: 7aa8681e5a75367cd2f370b339527d9aa80d6662

tech-stack:
  added: []
  patterns:
    - "Shared D-15 assertions live in the Docker-free unit class (package-private statics) and the live class calls them, so the two layers cannot drift"
    - "Live red-before in a throwaway worktree at the parent of the fix commit, with only the new test files and ThrowableTexts copied in"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java
    - .planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md
    - .planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md

key-decisions:
  - "QUERY_ID_MESSAGE and the rendered-fragment builder sit in ValueFreeDlqHeadersUnitTest (package-private) instead of the live class, so the live and Docker-free layers share one copy of the D-15 assertions (DRY); the live LiveCase delegates to it"
  - "The live Int128 case uses a Nullable(Int128) column, because validateDataSchema skips a nullable root column and only then does the STRING value reach the conversion"

patterns-established:
  - "LiveCase enum: each case carries its DDL, column, Type token, original class, value and batch, and two parameterized tests run over it"

requirements-completed: [WRT-13]

coverage:
  - id: D1
    description: "On ClickHouse latest x client V2, through the real ClickHouseSinkTask.put with errors.tolerance=none, an invalid UUID and an unparseable Int128 fail put with a chain, a UTF-8 rendering and an Error inserting records log that name the column, the Type token and the original class and hold no record value; no ServerException is in the chain or the logged throwable lines; no row lands"
    requirement: WRT-13
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java#WRT-13 live: errors.tolerance=none fails put value-free for INVALID_UUID / UNPARSEABLE_INT128 (2 cases)"
        status: pass
    human_judgment: false
  - id: D2
    description: "D-15: under errors.tolerance=all the DLQ holds every record and, rendered as Connect 4.1.1 renders them, exception.class.name is java.lang.RuntimeException, exception.message fully matches Processing's QueryIdentifier text with no column text, and exception.stacktrace names the column, type and class; the value is in neither header. Live on latest x V2 and Docker-free on V1 and V2"
    requirement: WRT-13
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java#WRT-13 live: errors.tolerance=all sends value-free DLQ headers for INVALID_UUID / UNPARSEABLE_INT128 (2 cases)"
        status: pass
      - kind: unit
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java#WRT-13 DLQ headers: tolerance all, clientV2={false,true} (2 cases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Every new case was seen red at the parent of fix(22-03) (6ecfae89) and green on HEAD, and the existing live cases pass at both; no production code changed after fix(22-03)"
    requirement: WRT-13
    verification:
      - kind: other
        ref: "/tmp/gsd22/exec/live-red-wep.xml, live-red-dlq.xml, live-green-wep.xml, live-green-dlq.xml; git diff --quiet c713a837 HEAD -- src/main"
        status: pass
    human_judgment: false
  - id: D4
    description: "Flagged assumptions A3 and A4: Connect's task status and its DeadLetterQueueReporter header algorithm are reproduced by the test (printStackTrace of the exception put() throws; class name, getMessage, printStackTrace in UTF-8) because connect-runtime is not on the test classpath"
    verification: []
    human_judgment: true
    rationale: "The real Connect runtime is not on this classpath; the reproduction follows the 4.1.1 source read in 22-RESEARCH F3, and the verifier may want stronger evidence (a real Connect worker) for the DLQ header claim"

duration: 6min
completed: 2026-10-19
status: complete
---

# Phase 22 Plan 04: Live put() and DLQ Header Pins for Value-Free Conversion Failures Summary

**Live ClickHouse latest x client V2 tests now pin that an invalid UUID and an unparseable Int128 fail `ClickHouseSinkTask.put` value-free in the thrown chain, its rendering and the `Error inserting records` log, and that the DLQ headers keep the queryId message while the stack-trace header names column, type and class; all seen red at the parent of the WRT-13 fix.**

D-15 reading: the DLQ `exception.class.name` stays `java.lang.RuntimeException` and `exception.message` stays Processing's queryId text with no column, while `exception.stacktrace` now names the column, the type and the class and carries no value.

D-13 reading: a client-side conversion failure always lands in `doInsertRawBinary`'s generic catch (V1 raw exception, V2 wrapped in `ClickHouseException`), never the `ServerException` branch, so SC2's two logs are met by the generic-branch log plus the written stack trace; a live tripwire asserts no `ServerException` appears.

## Performance

- **Duration:** 6 min
- **Started:** 2026-10-19T20:39:34Z
- **Completed:** 2026-10-19T20:45:30Z
- **Tasks:** 2 (tracer, expansion plus docs)
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments
- Tracer (WRT-13 live, D-12): the invalid-UUID tolerance=none case on the real client and server. At the parent of `fix(22-03)` (`6ecfae89`) it failed with the value in the thrown chain and the six existing cases passed; on HEAD all 7 pass.
- Expansion: the unparseable Int128 case (a `Nullable(Int128)` column so the value reaches the conversion), the tolerance=all DLQ header cases, and `ValueFreeDlqHeadersUnitTest` on V1 and V2. At `6ecfae89`: 4 of 4 `WRT-13 live` cases and 2 of 2 Docker-free cases failed, the 6 existing live cases passed. On HEAD: 10 of 10 live and 2 of 2 unit pass.
- `InMemoryDLQ` gained `exceptions()` and `connectHeaders(Throwable)`, additive only (28 insertions, 0 deletions), reproducing Connect 4.1.1's three exception headers.
- Ledger L-48 added and live pins appended to L-42 and L-43; E-04 records the runs, the D-15 reading and the D-13 trace table. No production code changed after `fix(22-03)`.

## Task Commits

1. **Task 1: tracer, invalid UUID on latest x V2 with errors.tolerance=none** - `f9b2cd52` (test)
2. **Task 2: Int128 case, DLQ header cases, InMemoryDLQ helper** - `0096a5fe` (test)
3. **Task 2: L-48, D-15 reading, E-04** - `7aa8681e` (docs)

**Plan metadata:** committed separately after this SUMMARY (docs: complete plan)

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/WriterErrorPathLiveTest.java` - `LiveCase` enum (INVALID_UUID, UNPARSEABLE_INT128), the tolerance=none and tolerance=all parameterized tests, UUID and Int128 batches
- `src/test/java/com/clickhouse/kafka/connect/sink/dlq/InMemoryDLQ.java` - `exceptions()`, `ConnectDlqHeaders`, `connectHeaders(Throwable)`
- `src/test/java/com/clickhouse/kafka/connect/sink/ValueFreeDlqHeadersUnitTest.java` - Docker-free D-15 test plus the shared assertions and `QUERY_ID_MESSAGE`
- `.planning/phases/22-writer-record-data-exposure/22-BEHAVIOUR-LEDGER.md` - L-48, live pins on L-42 and L-43
- `.planning/phases/22-writer-record-data-exposure/22-EVIDENCE.md` - E-04

## Decisions Made
- Shared D-15 assertions and `QUERY_ID_MESSAGE` live in the Docker-free unit class and the live class calls them (DRY). The plan listed `QUERY_ID_MESSAGE` among the live class's constants; nothing in the verify blocks depends on where it sits.

## Deviations from Plan

**1. [Rule 1 - Plan arithmetic] The plan counts 7 existing live cases; the class has 6**
- **Found during:** Task 1 (red run at `6ecfae89`: 7 cases in total, 6 existing plus the new one)
- **Issue:** `WriterErrorPathLiveTest` has 6 `@Test` methods at `fefd8648` and at HEAD. The plan's verify thresholds assumed 7 existing cases (`len(red)>=8` after Task 1, `len(gw)>=11` after Task 2), which can never hold: the class totals 7 after Task 1 and 10 after Task 2 (6 plus 2 plus 2).
- **Fix:** Ran the same checks with the thresholds corrected to the real counts (`>=7` after Task 1, `>=10` after Task 2). Every other condition (the new cases red, the existing cases green at the pre-fix commit, equal case counts between runs, nothing failing or skipped on HEAD) was kept as written. No test was added to hit a number.
- **Files modified:** none (verification thresholds only)
- **Verification:** `LIVE-TRACER-GREEN` printed with `>=7`; the discrimination check printed `wrong_in_red [] live_new 4 dlq_unit 2 green_cases 12 live_cases 10 green_bad []`.
- **Committed in:** not applicable (no code change); recorded in E-04 as `existing_live_cases: 6`

---

**Total deviations:** 1 (plan arithmetic, verification thresholds only)
**Impact on plan:** None on scope or on the evidence. The checks that ran are stricter than the corrected thresholds require on every discriminating condition.

## Issues Encountered
- A one-off shell mistake while re-running the anchors mirror under zsh: the plan's `read -r path` loop overwrites the tied `PATH` in zsh, so `bash` and `git` vanished for that one command. The mirror itself had already printed `rows=65 bad=0`; the verify script was re-run under bash (`E04-RECORDED`). No effect on the repository.
- No OpenJ9 crash occurred in any Gradle run, so no core dumps needed cleanup.

## Known Stubs

None.

## Threat Flags

None. Test code and docs only; no network endpoint, auth path, file access or schema change.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 22-05 owns the full suite on `latest` x V2 and the phase gate. The flagged assumptions A3 and A4 (Connect runtime rendering) are recorded as `backstop` truths and routed to a human in this plan's coverage block (D4).
- Reminder carried from 22-03 for the orchestrator: the second flip, `schemaWithFixedStringMismatchTest`, is still to be surfaced to the user.

## Self-Check: PASSED

The created file `ValueFreeDlqHeadersUnitTest.java` and the two modified test files exist, commits `f9b2cd52`, `0096a5fe` and `7aa8681e` are ancestors of HEAD, `git worktree list` shows one entry, `src/main` is unchanged since `fix(22-03)`, the WRT-14 pin, `ScriptedWriteFailureWriter` and `CountingHelperClient` are byte-identical to `0590d20c`, and LIVE-TRACER-GREEN (corrected threshold), the discrimination check, TESTUNIT-GREEN, SPOTLESS-BASELINE-EQUAL and E04-RECORDED all printed.

---
*Phase: 22-writer-record-data-exposure*
*Completed: 2026-10-19*
