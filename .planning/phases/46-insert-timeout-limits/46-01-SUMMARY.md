---
phase: 46-insert-timeout-limits
plan: "01"
subsystem: testing
tags: [stub-server, raw-socket, write-stall, progress-headers, baseline, evidence]

requires:
  - phase: 45-upstream-tail-merge-dependency-check
    provides: the sync/v1.7.0 tip 3d4ce2e8, the tail gate scripts, fork-anchors.txt (200 rows), the testUnit 2086 and full-suite 2689 expectations
provides:
  - 46-COMMAND-EVIDENCE.md with the header, the E-01..E-14 index and entries E-01..E-03
  - a green, attributable baseline on the untouched base B0 (anchors, gate self-test, tail gate, testUnit, one full suite on latest x V2)
  - the raw-socket mode of StubClickHouseServer with four factories (notReadingBody, readingBodySlowly, progressHeaders, progressWhileReadingBodySlowly) and bodyBytesRead()
  - StubClickHouseServerRawModeUnitTest, six Docker-free premise cases
  - research assumption A1 measured on macOS and on the colima VM's Linux kernel (53.9x margin for the 64 MiB pin body)
affects: [46-02, 46-03, 46-04, 46-05]

actuals:
  tokens: 11404
  tasks: 2
  commits: 5
plan_head_before: 3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5
plan_head_after: bde83fba33db131fa820223d83da5bbd0c82ece4

tech-stack:
  added: []
  patterns: [raw ServerSocket stub next to the com.sun HttpServer mode, loopback receive buffer set before bind, premise test per stub behaviour]

key-files:
  created:
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
  modified:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java

key-decisions:
  - "The raw mode is a second private constructor of the existing class (RawHandler lambda per factory), not a second class, so port(), requests() and close() are shared and the HTTP mode and its six users stay byte-for-byte unchanged in behaviour"
  - "Slow body reads are paced per tick (a loop of reads until bytesPerTick bytes, then one sleep), so the pace does not depend on how many bytes one socket read returns from a 4096-byte receive buffer"
  - "close() resets every accepted socket (linger 0) and interrupts the raw threads, so a client blocked in a body write fails at once"

requirements-completed: [TMO-03, TMO-02]

coverage:
  - id: D1
    description: "Phase 46 baseline on the untouched base B0 (3d4ce2e8): anchors rows=200 bad=0, gate self-test clean, unedited tail gate PASS with anchor_rows=200, cache-busted testUnit 2086 tests green (2 skips, three gg7 timeout pin classes green), one full suite 2689 tests green on ClickHouse 26.10.3.7 x client V2 with the 18 named skips, digest recorded"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-01-T1-OK (Task 1 automated verify; E-01)"
        status: pass
    human_judgment: false
  - id: D2
    description: "StubClickHouseServer raw mode with notReadingBody() and bodyBytesRead(); a 64 MiB chunked body write is still blocked after 2 s with 0 body bytes read and 1 recorded request, and ends with an IOException within 5 s of close(); the HTTP mode and its five existing test classes are green"
    requirement: TMO-03
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java#notReadingBodyBlocksA64MebibyteBodyWrite"
        status: pass
      - kind: other
        ref: "P46-01-T1-OK (E-02)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The other three raw responders (readingBodySlowly, progressHeaders, progressWhileReadingBodySlowly), each pinned by a premise case; the test class has six green cases"
    requirement: TMO-02
    verification:
      - kind: test
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java"
        status: pass
      - kind: other
        ref: "P46-01-T2-OK (E-03)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Research assumption A1 measured: loopback bytes absorbed before a write blocks are 662096 on macOS and 1245184 on Linux 6.8.0-50 (colima vz), so the 64 MiB pin body is 53.9x the larger value"
    requirement: TMO-03
    verification:
      - kind: other
        ref: "P46-01-T2-OK (E-03 a1_margin 53.9, minimum 4.0)"
        status: pass
    human_judgment: false
---

# Phase 46 Plan 01: Raw-Socket Stub Harness and Attributable Baseline Summary

**StubClickHouseServer gains a raw-socket mode (a peer that reads only the head, reads the body slowly, streams progress headers, or streams them while still reading) pinned by six Docker-free premise cases, after a green baseline on the untouched base and a 53.9x measured margin for the 64 MiB write-stall body.**

## Performance

- **Duration:** about 70 min (about 63 min of it the gate self-test, whose ST4 step runs the cumulative gate; the baselines and the harness work ran alongside it)
- **Started:** about 2026-11-05T08:50:00Z
- **Completed:** 2026-11-05T10:00:00Z
- **Tasks:** 2
- **Files modified:** 3 (two test-harness files and the evidence file); `src/main` untouched

## Names later plans use

- **B0** = `3d4ce2e864506a5b68c0c2e2ac4400a3e0d886f5` (E-01 `local_head`, the phase base).
- **R0** = `9bd0944ebd066852ef059e7b2bca2a6ff2a54137` (E-03 `harness_tip`); `git diff --name-only B0 R0 -- src/main` is empty, and under `src` only the two harness files differ.
- Commits: `2b7ffb7e` (E-01), `d1cbdff2` (raw slice), `57a240b7` (E-02), `9bd0944e` (R0, remaining responders), `bde83fba` (E-03).

## Baselines on the untouched base (the totals later plans compute expected counts from)

- Cache-busted `testUnit` (default toolchain, semeru-17.0.9 OpenJ9): **2086 tests, 0 failures, 0 errors, 2 skipped, 98 files**. gg7 pins: `InsertStallBoundPinUnitTest` 15, `InsertSocketTimeoutOptionUnitTest` 23, `RetriableExceptionPassThroughUnitTest` 38, all green.
- Full suite on ClickHouse `latest` x client V2: **2689 tests, 0 failures, 0 errors, 18 skipped, 147 files, 172 s**; `skips` prints `OK skips: named=7 state_mismatch_cases=11 unexpected=0`. `SELECT version()` = **26.10.3.7**, digest `clickhouse/clickhouse-server@sha256:739e099abd1157390960a59e4532d21781d8fde57e6e4151cb920ea02c30b1ce` (unchanged since Phases 41, 44 and 45).
- Anchors mirror `rows=200 bad=0`; helper md5 `7123d354f51d45e019837f43b3a30fbc`; `grep -rn async_insert src/main | wc -l` = 2; gate self-test `selftest: no failures`; unedited tail gate over `12e2cf17..B0`: `provenance_paths=11 anchor_commits=51 anchor_rows=200 marker_commits=69 anchor_exempt=0 result=PASS`, exit 0, 3 min 4 s.
- XML snapshots for later gate verifies: `/tmp/gsd46/exec/xml-base-testunit` and `/tmp/gsd46/exec/xml-base-suite`.

## Accomplishments

- **E-01.** The base is measured green and attributable: every later red belongs to this phase.
- **Raw mode (E-02, E-03).** `StubClickHouseServer` has a second private constructor over a `ServerSocket` on `127.0.0.1:0` with a 4096-byte receive buffer set before bind (`RAW_RECEIVE_BUFFER_BYTES`), daemon accept and connection threads, one byte-by-byte head reader (so no body byte is consumed beyond the head), a shared body reader for chunked and Content-Length bodies with the `100-continue` answer, and `bodyBytesRead()`. Factories: `notReadingBody()`, `readingBodySlowly(int, Duration)`, `progressHeaders(Duration, Duration)` (one `X-ClickHouse-Progress` line per interval until the total elapses, then the summary, `Content-Length: 0` and the blank line), `progressWhileReadingBodySlowly(int, Duration, Duration)` (200 at once, progress while the body is still read). `close()` resets every accepted socket and interrupts the raw threads.
- **Premise pins.** `StubClickHouseServerRawModeUnitTest` (6 cases, no tag, no mocking library, `@Timeout(60)`): the 64 MiB write is still blocked after 2 s with 0 body bytes read and 1 recorded request and ends with an `IOException` within 5 s of `close()` (2.0 s); request line and headers are recorded; the slow reader keeps reading (increasing, below 8 MiB) without answering; the progress responder streams at least 8 lines spread over at least 700 ms for both a chunked and a Content-Length body and records the body; the progress-while-reading responder sends the status line and at least 3 progress lines while the body is still being read. Repeated three more times in a row, all green.
- **A1 (E-03).** A probe with `SO_RCVBUF` 4096 before `listen`, a peer that reads only the head, and a 2 s send timeout absorbed **662096 bytes on macOS** (Darwin 25.5.0) and **1245184 bytes on the colima VM** (Linux 6.8.0-50-generic; four identical runs each). The 64 MiB body is 53.9x the larger value (the plan's floor is 4.0), so the write-stall pins of 46-02 and 46-03 cannot be swallowed by loopback buffers on either OS.

## Task Commits

1. **Task 1 (tracer): baseline and first slice** - `2b7ffb7e` (E-01), `d1cbdff2` (notReadingBody and its two premise cases), `57a240b7` (E-02)
2. **Task 2: remaining responders and A1** - `9bd0944e` (R0), `bde83fba` (E-03)

**Plan metadata:** the docs(46-01) completion commit follows this file (SUMMARY only; STATE.md, ROADMAP.md and REQUIREMENTS.md are left to the orchestrator, per the dispatch instructions).

`actuals.commits: 5` is the measured `git rev-list --count plan_head_before..plan_head_after`; `actuals.tokens` is chars/4 over the added lines under `src` plus the evidence file.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java` - raw-socket mode and four factories (HTTP mode unchanged)
- `src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java` - six premise cases
- `.planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md` - header, index E-01..E-14, entries E-01..E-03

## Decisions Made

See `key-decisions` above. No decision belonging to the user came up; U-1..U-7 are untouched by this plan.

## Deviations from Plan

### Auto-fixed Issues

None. Observations, none a rule trigger:

- **Sequencing.** The gate self-test takes about an hour and moves HEAD-relative steps if a commit lands during it, so no commit was made until it finished. The testUnit and full-suite baselines ran on a tree with `src` equal to B0 while it ran, the raw-stub code was written and tested in the working tree meanwhile, and the commits were then made in the plan's order (E-01, raw slice, E-02, remaining responders, E-03). The raw slice was run on the exact committed slice contents before the E-01 commit and again after it; the recorded figures are from the repeat. Committed file contents were the ones tested and Spotless-checked.
- **Pacing implementation.** The plan says the slow reader reads "at most `bytesPerTick` body bytes per `tick`". With a 4096-byte receive buffer one socket read returns only a few KB, so a single read per tick would run at about 200 KB/s instead of 3 MB/s; the implementation loops reads until `bytesPerTick` bytes are consumed and then sleeps one tick. The stated upper bound still holds.
- **macOS receive buffer.** On macOS the accepted socket reports a 326568-byte receive buffer, so the 4096-byte request is not honoured there (Linux reports 8192). The pin does not depend on it: the 64 MiB body blocks on macOS (Java premise test, 2.0 s) and on Linux (probe), with margins of 101x and 53.9x. Recorded in E-03.

**Total deviations:** 0 auto-fixed. **Impact:** none.

## Issues Encountered

- The first full-suite attempt on the baseline ended with the known intermittent OpenJ9 crash (`Gradle Test Executor 7 finished with non-zero exit value 255`, 2587 of 2689 tests had passed, no failure among them). The four crash files (`core.*.dmp`, `javacore.*`, `Snap.*`, `jitdump.*`) were deleted by name and the single re-run was green (exit 0, 2689 tests). Both runs are recorded in E-01. No crash recurred in later runs.
- The gate self-test took about 63 minutes (expected: ST4 runs the cumulative gate).

## User Setup Required

None - no external service configuration required.

## Threat Flags

None. The raw stub listens only on `127.0.0.1` in test code; no production file changed.

## Next Phase Readiness

- Ready for plan 46-02: R0 `9bd0944e` is the harness tip, byte-identical to B0 under `src/main`, so a worktree at R0 is the unchanged production tree for red-first runs.
- Expected counts for later plans start from testUnit 2086 and full suite 2689; the raw pin class adds 6 cases to both (testUnit 2092 and suite 2695 once this plan's six cases are counted).
- The anchors count stays 200 until plan 46-02 adds its rows (208 expected).

## Self-Check: PASSED

- Files: `46-COMMAND-EVIDENCE.md`, `StubClickHouseServer.java` and `StubClickHouseServerRawModeUnitTest.java` present.
- Commits `2b7ffb7e`, `d1cbdff2`, `57a240b7`, `9bd0944e`, `bde83fba` are ancestors of HEAD.
- Verify blocks P46-01-T1-OK and P46-01-T2-OK printed on the committed tip; `git diff B0 HEAD -- src/main build.gradle.kts gradle .github` is empty; anchors mirror `rows=200 bad=0`.
