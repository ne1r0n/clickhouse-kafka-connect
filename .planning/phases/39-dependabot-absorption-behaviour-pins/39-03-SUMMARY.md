---
phase: 39-dependabot-absorption-behaviour-pins
plan: "03"
subsystem: testing
tags: [pin-tests, proxy-sink-task, error-classification, client-version-tripwire, custom-keys, fork-delta, docker-free]

# Dependency graph
requires:
  - phase: 39-dependabot-absorption-behaviour-pins
    provides: "plans 39-01 and 39-02: a tree holding both Dependabot merges, E-01..E-05 and the evidence helper"
provides:
  - "WrappedDataExceptionClassificationPinUnitTest: a 14-case matrix (7 shapes x errors.tolerance none/all) through the real ProxySinkTask.put, passing on unchanged src/main"
  - "customKeysAreForwardedWithDoubledEncodingClientVersionTripwire: a Docker-free client-version tripwire for custom_* forwarding on client 0.11.0 (and no forwarding on 0.9.6)"
  - "FORK-DELTA Known cost 17 rewritten to state the pinned put-layer outcomes and name the class"
  - "39-COMMAND-EVIDENCE.md entries E-06 and E-07, a testUnit baseline of 1687 tests with both pins"
affects: [39-04, 41-timeout-classification, 42-retry-budget, 44-release-rehearsal]

requires_next:
  - "Phase 41 (TMO-02) and Phase 42 (RSK-02) measure their classification changes against the PIN-02 table; the next client bump trips PIN-03"

# Actuals (#2632): chars/4 over the realized diff (STATE.md excluded, evidence file included)
actuals:
  tokens: 5800
  tasks: 2
  commits: 4
plan_head_before: b4441e5a494a0e10367da26bc18aca01d5080317
plan_head_after: 8b75821412f9136124edf87d3cd0ac00bead73f6

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Failure injection by subclassing InMemoryDBWriter and driving the package-private ProxySinkTask(config, reporter, DBWriter) constructor; no mocking framework"
    - "Client-version tripwire: switch on ClientErrorShapes.line() read inside assertDoesNotThrow, compare parsed raw query values (order-insensitive), never the raw URI"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java
    - docs/FORK-DELTA.md
    - .planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md

key-decisions:
  - "Expectations are literal rows written from the research measurement, never derived from the code under test; every row passed first time, so none was edited"
  - "The conversion-time DataException is pinned as a labelled control (ESCAPES_RAW under both tolerances, writer never called, DLQ empty)"
  - "The PIN-03 0.9.6 branch asserts only that no custom_* key is forwarded; it is the Phase 36 E-11 measurement and is not re-measured because 0.9.6 is not on this classpath"

patterns-established:
  - "One parameterized test with an Outcome enum (RETRIABLE, FAILS_TASK, DLQ, ESCAPES_RAW, DROPPED) classifies put's behaviour; RetriableException is caught before DataException before RuntimeException so a retriable result is never mistaken for a task failure"

requirements-completed: [PIN-02, PIN-03]

coverage:
  - id: D1
    description: "Through the real ProxySinkTask.put, a plain writer DataException fails the task under none and reaches the DLQ under all; a DataException with a retriable server code 241 or a socket-timeout root is a RetriableException under both tolerances with an empty DLQ"
    requirement: PIN-02
    verification:
      - kind: unit
        ref: "WrappedDataExceptionClassificationPinUnitTest#wrappedDataExceptionOutcomeThroughProxySinkTaskPut: 14 tests, 0 failures, 0 errors, 0 skipped, Docker unreachable (E-06)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The controls hold: non-retriable code 60 fails the task or goes to the DLQ, an UnknownHostException root and a RuntimeException with a socket-timeout root are retriable under both tolerances, and a conversion-time DataException escapes put raw under both with the writer never called"
    requirement: PIN-02
    verification:
      - kind: unit
        ref: "the same 14-case class (E-06)"
        status: pass
    human_judgment: false
  - id: D3
    description: "FORK-DELTA Known cost 17 names the pin and states the put-layer outcomes in the same commit as the class; the clause saying no test asserts the cases is gone"
    requirement: PIN-02
    verification:
      - kind: other
        ref: "grep WrappedDataExceptionClassificationPinUnitTest docs/FORK-DELTA.md; grep -c 'no test asserts them' = 0; h expect E-06 (P39-03-T1 checks)"
        status: pass
    human_judgment: false
  - id: D4
    description: "On client 0.11.0 the V2 client forwards custom_myparam=7, custom_settings=max_threads%253D3 and custom_a=b%2520c, drops ssl and sends a query key; the test, its display name and its failure message call it a client-version tripwire naming docs/ROLLOUT-v1.7.0_gg6.md row D7"
    requirement: PIN-03
    verification:
      - kind: unit
        ref: "ClientEndpointAndOptionsPinUnitTest#customKeysAreForwardedWithDoubledEncodingClientVersionTripwire: class 5 tests, 0 failures (E-07)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The full testUnit with --rerun and Docker unreachable passes with exactly 15 more tests than E-05 and 2 skipped; src/main is unchanged since the phase start"
    requirement: PIN-03
    verification:
      - kind: unit
        ref: "gradlew testUnit --rerun: 1687 tests, 0 failures, 0 errors, 2 skipped (E-07, P39-03-T2 checks)"
        status: pass
    human_judgment: false

# Metrics
duration: 5min
completed: 2026-11-01
status: complete
---

# Phase 39 Plan 03: PIN-02 and PIN-03 behaviour pins Summary

**A 14-case matrix pins how `ProxySinkTask.put` classifies seven writer-failure shapes under both `errors.tolerance` values, and a client-version tripwire pins client 0.11.0's doubled-encoding forwarding of `custom_*` keys; both are Docker-free, mock-free, and pass on unchanged `src/main`.**

## Performance

- **Duration:** 5 min
- **Started:** 2026-11-01T19:19:39Z
- **Completed:** 2026-11-01T19:25Z
- **Tasks:** 2
- **Files modified:** 4 (one new test class, one edited test class, FORK-DELTA, the evidence file)

## Accomplishments

- Tracer (E-06): `WrappedDataExceptionClassificationPinUnitTest` drives the real `ProxySinkTask.put` with a `FailingDBWriter` (a subclass of `InMemoryDBWriter`) and an `InMemoryDLQ`. Its 14 rows pass as written. A plain writer `DataException` fails the task under `none` (`Number of records: 3`, the injected exception under the exact-`RuntimeException` queryId wrapper) and reaches the DLQ under `all`. A `DataException` that wraps server code 241, or has a socket-timeout root, is a `RetriableException` under both values with an empty DLQ. The controls (code 60, `UnknownHostException`, a `RuntimeException` with a socket-timeout root, and a conversion-time `DataException` that escapes `put` raw) all match the research table.
- FORK-DELTA Known cost 17 now states these put-layer outcomes and names the class, in the same commit as the class. The FUT-05 sentence, owner and re-open trigger are kept; the clause that said no test asserts the cases is gone.
- PIN-03 (E-07): one added test in `ClientEndpointAndOptionsPinUnitTest`. On 0.11.0 it asserts from parsed raw query values, not the raw URI, that `custom_myparam` is `7`, `custom_settings` is `max_threads%253D3`, `custom_a` is `b%2520c`, there is no `ssl` key and there is a `query` key. The 0.9.6 branch asserts no `custom_*` key is forwarded. An unknown client line fails inside `assertDoesNotThrow` with the `client-version tripwire` text that names `docs/ROLLOUT-v1.7.0_gg6.md` row D7.
- Baseline (E-07): the full `testUnit` with `--rerun` and Docker unreachable ran 1687 tests, 0 failures, 0 errors, 2 skipped, which is E-05's 1672 plus 15.

## Task Commits

1. **Task 1 (tracer): PIN-02 matrix with FORK-DELTA Known cost 17** - `6f067dba` (test), evidence `a30e7c9a` (docs)
2. **Task 2: PIN-03 client-version tripwire and the testUnit baseline** - `59c5975e` (test), evidence `8b758214` (docs)

**Plan metadata:** the SUMMARY commit, followed by the STATE and ROADMAP commit (docs).

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/WrappedDataExceptionClassificationPinUnitTest.java` - new; one parameterized test, nested `FailingDBWriter`, enums `Shape` and `Outcome`, helpers `parentOf` and `chainContains`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/ClientEndpointAndOptionsPinUnitTest.java` - four constants, `rawQueryMap`, the tripwire test and three imports added; no line removed
- `docs/FORK-DELTA.md` - Known cost 17 first sentence replaced by the pinned facts
- `.planning/phases/39-dependabot-absorption-behaviour-pins/39-COMMAND-EVIDENCE.md` - entries E-06 and E-07, index rows flipped to pass

## Decisions Made

- Every expectation is a literal row from research F6 and F7; all rows passed first time, so no expectation was changed to match the code.
- The conversion-time `DataException` is included as a labelled control (orchestrator decision 2), since it is the one shape that never reaches `Utils.handleException`.
- The 0.9.6 branch of the tripwire is the Phase 36 E-11 measurement, not re-measured, because 0.9.6 is not on this classpath.

## Deviations from Plan

### Execution notes (no deviation rule applies; no code behaviour changed)

**1. [Evidence accuracy] E-06 closing timestamp corrected**
- **Found during:** Task 2, while writing E-07
- **Issue:** E-06's closing timestamp first read `2026-11-01T19:25:00Z`, an estimate typed by hand that is later than its own commit `a30e7c9a` (19:21:35Z).
- **Fix:** corrected to the `date -u` reading `19:21:00Z` in the E-07 commit and stated in E-07's "Correction note". No result, key or verdict of E-06 changed.
- **Files modified:** `39-COMMAND-EVIDENCE.md`
- **Commit:** `8b758214`

**2. [Gate form] PIN-03 commit amended before it was recorded**
- **Found during:** Task 2, first run of the automated verify
- **Issue:** the plan gate greps `assertDoesNotThrow(ClientErrorShapes::line` on one line, and my line wrap put the first argument on the next line, so the grep failed.
- **Fix:** whitespace-only reformat, the scoped Spotless check re-run (IS CLEAN), the unpushed commit `aaa1bacc` amended to `59c5975e` so `pin03_commit` still names a single-file commit, and the class test and the full testUnit re-run on it. E-07 records the amend.
- **Files modified:** `ClientEndpointAndOptionsPinUnitTest.java`
- **Commit:** `59c5975e`

**Total deviations:** 0 under the deviation rules; 2 execution notes above. **Impact:** none on behaviour or results.

One more execution note, as in 39-02: `h` is an alias for `history` in this zsh, so the plan's `h() {...}` blocks ran under `/bin/bash` with a sourced helper function (`/tmp/gsd39/exec/h.sh`, scratch, not committed).

## Issues Encountered

None. No JVM crash file appeared in the repository root and no run needed a retry. Nothing was pushed.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. The plan adds test code and one documentation paragraph; the PIN-03 test talks only to a loopback stub with synthetic keys, every exception message is a constant, and no new endpoint, auth path or schema surface appears.

## Next Phase Readiness

- Ready for plan 39-04 (PIN-04 Datagen harness fix and the phase gate). `src/main` is unchanged against `phase_start_sha`, the sibling TAL-04 pin is byte-unchanged, and the testUnit baseline with both pins is 1687 tests with 2 skipped.
- Phase 41 and Phase 42 can measure classification changes against the E-06 table; the next ClickHouse client bump fails the PIN-03 tripwire on purpose.

## Self-Check: PASSED

- Created file exists: the PIN-02 test class; modified files exist: the CLT-03 test class, FORK-DELTA, the evidence file.
- Commits exist and are ancestors of HEAD: `6f067dba`, `a30e7c9a`, `59c5975e`, `8b758214`.
- Task automated verifications passed (the T1 checks before the evidence commit, the T2 checks after the amend): `h expect` E-06 (6 checks) and E-07 (11 checks) report 0 failed, `h rows` E-06 E-07 ok, `git diff --quiet phase_start_sha HEAD -- src/main` exits 0.

---
*Phase: 39-dependabot-absorption-behaviour-pins*
*Completed: 2026-11-01*
