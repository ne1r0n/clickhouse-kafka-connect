---
phase: 46-insert-timeout-limits
verified: 2026-11-05T13:30:00Z
status: passed
score: 4/4 must-haves verified
covered_files:
  - ".planning/phases/46-insert-timeout-limits/46-01-PLAN.md"
  - ".planning/phases/46-insert-timeout-limits/46-01-SUMMARY.md"
  - ".planning/phases/46-insert-timeout-limits/46-02-PLAN.md"
  - ".planning/phases/46-insert-timeout-limits/46-02-SUMMARY.md"
  - ".planning/phases/46-insert-timeout-limits/46-03-PLAN.md"
  - ".planning/phases/46-insert-timeout-limits/46-03-SUMMARY.md"
  - ".planning/phases/46-insert-timeout-limits/46-04-PLAN.md"
  - ".planning/phases/46-insert-timeout-limits/46-04-SUMMARY.md"
  - ".planning/phases/46-insert-timeout-limits/46-05-PLAN.md"
  - ".planning/phases/46-insert-timeout-limits/46-05-SUMMARY.md"
  - ".planning/phases/46-insert-timeout-limits/46-06-SUMMARY.md"
  - ".planning/scripts/fork-anchors.txt"
  - "docs/FORK-DELTA.md"
  - "src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java"
  - "src/main/java/com/clickhouse/kafka/connect/sink/db/helper/ClickHouseHelperClient.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/InsertBoundPinFixtures.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/InsertV1WaitBoundPinUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/InsertBoundGateUnitTest.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java"
  - "src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java"
covered_digest: "v4:sha256:0ec29df4e99157acebdd6cd29e849c711d9047da0a03506a07211bcd1b1ea712"
behavior_unverified: 0
overrides_applied: 0
advisory:
  - finding: "WR-05 (open in 46-REVIEW-DISPOSITION.md): a V2 deadline cancel on the RowBinary route logs two ERROR lines that blame one record/column before the RetriableException is thrown"
    category: other
    reason: "Misleading operator log, not a classification or bound failure; the user-visible exception is correct. Recorded as a backlog candidate by the disposition."
    evidence_status: "reproducible from the code (ClickHouseWriter doWriteColValue / writeColumnWithRecordContext catch blocks), no test asserts it"
  - finding: "IN-05 (V1 timeout path ignores cancel(true) result) and IN-06 (stop() during an insert can surface RejectedExecutionException from arm()) remain open"
    category: other
    reason: "Info-level, outside the four success criteria; stop() race only."
    evidence_status: "code reading only"
---

# Phase 46: Insert Timeout Limits Verification Report

**Phase Goal:** An operator can trust what `clickhouseClientInsertTimeoutMs` says. The gg7 fix bounded a stalled client V2 `async=false` insert and left four cases open (progress-header streaming insert, write-side stall, client V1 `future.get()` inserts, non-insert requests). Each is now either bounded (failing within the bound as a retriable error) or written down as a limit with a measured number. The help text names exactly what the setting bounds.
**Verified:** 2026-11-05T13:30:00Z
**Status:** passed
**Re-verification:** No, initial verification (no previous VERIFICATION.md)

## Goal Achievement

### Observable Truths (ROADMAP success criteria, the contract)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | *[live, record]* A progress-header streaming insert is bounded or recorded with a measured number from a real `latest` server (TMO-02) | VERIFIED | U-1 chose "record". FORK-DELTA item 14 case (a) carries the numbers: 8299 ms against a 3000 ms bound on the fixed tree (8315 base, 8059 control), only with `async_insert=0` and 500-row blocks; default `async_insert=1` fails retriably at 3050 ms; ClickHouse 26.10.3.7, image digest in E-10. The limit is pinned by `progressStreamingV2InsertRunsPastTheBoundOnceTheBodyIsSent`. The write-while-sending sub-shape is bounded (stub pin 1.013 s) and FORK-DELTA states plainly that it rests on the stub pin, not on a live run. I did not re-run the Docker live probe (throwaway, never committed); the numbers are read from E-10 and match FORK-DELTA and the help text. |
| 2 | *[Docker-free, record]* Write-side stall, V1 `future.get()` sites and non-insert requests each bounded behind a red-first pin, or recorded with a measurement (TMO-03) | VERIFIED | **V2 write stall: bounded.** `InsertWriteDeadline` armed before `client.insert` at all three V2 sites, disarmed when the body closes, `client::cancelTransportRequest`, `RetriableException` naming the setting. **V1: bounded.** `awaitInsertResponseV1` at the three former `future.get()` sites (ClickHouseWriter 1615/1745/1900 callers; the only bare `future.get()` left is line 2010, inside the `bound <= 0` operator-gate branch). **Non-insert requests and V2 ping: recorded** by decision U-4/U-5/U-6 with E-11 numbers (20005 ms V2 still blocked at cap, 30034-30045 ms V1, V1 ping 2082 ms, V2 ping no-op confirmed) and FORK-DELTA item 14 (b),(c); the false "ping has its own timeout" claim is corrected. **Red-first re-run by me:** a detached worktree at `9bd0944e` (src/main byte-identical to base `3d4ce2e8`) with the V2 and V1 pin classes, fixtures and stub copied in gave 41 tests, 33 failed (15 V2 + 18 V1), matching E-04 and E-07. The V1 body-write stall inside the pipe was not reached up to 256 MiB and is recorded as not measured, not as bounded. |
| 3 | *[Docker-free]* Through `ProxySinkTask.put`, each bounded case is retriable under `errors.tolerance=none` and `all`, no DLQ record; operator `socket_timeout` wins; no new config key; `async_insert` untouched (TMO-02, TMO-03) | VERIFIED | `putMustBeRetriableWithNoDlqRecord` builds a real `ProxySinkTask` with `InMemoryDLQ`, parameterised over route x tolerance (V2: 6 + 2 put cases; V1: 10 put cases), asserts `RetriableException` thrown from `put` and `dlq.size()==0`. No Mockito in the pin classes. Operator gate: `ClickHouseHelperClient.insertBoundMs` returns 0 when `operatorSetSocketTimeout` (an explicit 0 included); `InsertBoundGateUnitTest` (20) and the operator cases in both pin classes pass. `ClickHouseSinkConfig` diff is only string literals inside the `CLICKHOUSE_CLIENT_INSERT_TIMEOUT_MS` define call (`git diff` read: 32 added, 7 removed, all in the help string); define count unchanged at 48; `async_insert` lines in `src/main` still 2; `clientBuilder.setSocketTimeout(` count 0 in the helper client. |
| 4 | *[Docker-free]* Help text written after 1-3, says what it bounds and does not, pinned Docker-free against the measured list; the config diff is that text only (TMO-01) | VERIFIED | The help text in `ClickHouseSinkConfig` has "What it bounds" / "What it does not bound" sections with V2, V1, non-insert, operator gate, `async=true` exception, rollout/duplicate risk, and states the V1 pipe-write stall as not bounded and unreached up to 256 MiB. `InsertTimeoutHelpTextPinUnitTest` (27 cases) passes; its `onlyTheDocumentationOfTheKeyChanged` case pins type/default/validator/importance. Review rows WR-02, WR-03, WR-04, IN-01, IN-04 (overclaims) were fixed in plan 46-06 with the pin updated first (13 of 27 red on the old text per E-17). |

**Score:** 4/4 truths verified (0 present-but-behavior-unverified: every behavior-dependent truth is carried by a named, passing, red-first test)

### Behavioral Spot-Checks (run by me, Docker unreachable)

Command: `env -u TESTCONTAINERS_HOST_OVERRIDE DOCKER_HOST=unix:///nonexistent.sock ./gradlew testUnit --rerun --tests` on the eight classes below.

| Class | Tests | Result |
|-------|-------|--------|
| InsertWriteStallBoundPinUnitTest | 20 | pass |
| InsertBoundGateUnitTest | 20 | pass |
| InsertV1WaitBoundPinUnitTest | 21 | pass |
| InsertTimeoutHelpTextPinUnitTest | 27 | pass |
| StubClickHouseServerRawModeUnitTest | 7 | pass |
| InsertStallBoundPinUnitTest (gg7) | 15 | pass |
| InsertSocketTimeoutOptionUnitTest (gg7) | 23 | pass |
| RetriableExceptionPassThroughUnitTest (gg7) | 38 | pass |
| **Total** | **171 / 171** | exit 0 |

Red-first reproduction: same V2 and V1 pin classes against unchanged `src/main` (worktree at `9bd0944e`): 41 tests, 8 pass, 33 fail by the `assertTimeoutPreemptively` guard or the watchdog assertion. The worktree was removed afterwards; the repo working tree is untouched.

Anchors mirror: `46-evidence-check.py anchors .planning/scripts/fork-anchors.txt --rows 216` -> `rows=216 bad=0`.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ClickHouseWriter.java` | V2 write deadline, V1 bounded wait, watchdog lifecycle | VERIFIED | `InsertWriteDeadline`, `deadlineAwareWriter` (with `onRetry` re-arm), `insertWithWriteDeadline`, `awaitInsertResponseV1`; watchdog is a daemon `ScheduledThreadPoolExecutor` with `setRemoveOnCancelPolicy(true)`, shut down in `stop()` |
| `ClickHouseHelperClient.java` | `insertBoundMs` operator gate | VERIFIED | 10-line addition, single gate for V1 and V2 |
| `ClickHouseSinkConfig.java` | help text only | VERIFIED | diff is string literals only |
| `docs/FORK-DELTA.md` item 14 + Connector row | recorded limits with numbers, U-6 wording | VERIFIED | "not bounded by decision" x4, evidence ids cited, ping claim corrected, rollout consequence stated |
| New pins and `StubClickHouseServer` raw mode | red-first, Docker-free, no Mockito | VERIFIED | classes present and green; premise tests prove the stub can block a 64 MiB write |

### Key Link Verification

| From | To | Status | Details |
|------|----|--------|---------|
| RowBinary, JSON, String V2 insert methods | `insertWithWriteDeadline` + `deadlineAwareWriter` | WIRED | grep shows all three `client.insert(` call sites (lines 1498, 1799/1800, 1948) go through it; no unwrapped V2 insert remains |
| Three V1 insert methods | `awaitInsertResponseV1` | WIRED | lines 1618, 1751, 1906 |
| `insertWriteDeadline` / `awaitInsertResponseV1` | `chc.insertBoundMs` | WIRED | both read the one gate |
| Fired deadline | `RetriableException` | WIRED | `throwIfFired` on the exception path only, so a fire racing a success returns the success |
| `ClickHouseWriter.stop()` | watchdog `shutdownNow` | WIRED | pinned by `insertWatchdogIsADaemonThreadEndedByStop` |
| `Processing`, `ProxySinkTask`, `Utils`, `KeeperStateProvider` | untouched | VERIFIED | `git diff --quiet 3d4ce2e8 HEAD` on those paths exits 0, so the `BEFORE`/`AFTER` flag sequence and classification are unchanged |

### Data-Flow Trace (Level 4)

Not applicable: no rendered dynamic data. The relevant flow (bound value from `csc.getClickhouseClientInsertTimeoutMs()` through `insertBoundMs` to the watchdog/`future.get(bound)`) is traced above and exercised by the pins with real values.

### Probe Execution

SKIPPED (no `scripts/*/tests/probe-*.sh`; the plans declare none). The Phase 34-45 tail gate (`verify-merge-invariants.sh`, 5 to 7 min) was not re-run by me; E-06, E-09, E-14 and E-19 record `result=PASS` with `anchor_rows` 208, 212, 214, 216 on explicit SHAs, and I re-ran the anchors mirror (216, bad 0).

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|----------------|-------------|--------|----------|
| TMO-01 | 46-05, 46-06 | Help text says what the setting really bounds; Docker-free test pins it against measured behaviour | SATISFIED | `InsertTimeoutHelpTextPinUnitTest` 27/27 green; text read against E-07..E-11; overclaims corrected in 46-06 |
| TMO-02 | 46-01, 46-02, 46-04, 46-05 | Progress-header insert bounded, or its escape re-measured against a real server and kept as a recorded limit with the number | SATISFIED | Recorded limit (U-1) with 8299/8315/8059/3050/8380 ms from E-10 on ClickHouse 26.10.3.7, in FORK-DELTA and help text; progress-while-sending sub-shape bounded by the V2 deadline (stub pin) |
| TMO-03 | 46-01, 46-02, 46-03, 46-04, 46-05, 46-06 | Write-side stalls, V1 `future.get()` inserts, non-insert requests each bounded with a red-first pin or recorded with a measurement | SATISFIED | V2 write stall bounded (deadline + retry re-arm), V1 bounded (3 sites), non-insert and V2 ping recorded with E-11 numbers; red-first reproduced by me (33 red on unchanged tree) |

No orphaned requirements: REQUIREMENTS.md maps only TMO-01..03 to Phase 46, and every plan's `requirements:` list is a subset of those. Housekeeping note: the REQUIREMENTS.md checkboxes and traceability rows for TMO-01..03 still read `Pending` and ROADMAP lists the 46 plan boxes unchecked; the phase-complete step should flip them (not a code gap).

### Recorded Limits (user decisions, not gaps)

- V1 body-write stall inside the 4 MiB pipe not reached up to 256 MiB; recorded as not measured, not as bounded (E-07, E-09).
- V2 insert whose body is sent and whose server keeps streaming progress headers runs to its natural end (U-1; 8299 ms vs 3000 ms bound); a V2 insert still sending its body at the bound is cut even with progress or a slow upload (U-7), with the `exactlyOnce=false` duplicate risk stated in help text and FORK-DELTA.
- Non-insert requests are unbounded on V2 (except the Keeper read at about 30 s) and bounded only by the library's per-attempt 30 s on V1 (U-4).
- V2 `ping(timeout)` is a no-op; item 14 corrected (U-5).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (phase-modified files) | - | `TBD`/`FIXME`/`XXX` debt markers | none | grep over the three `src/main` files for markers added by the phase found none |
| `ClickHouseWriter.java` | RowBinary catch blocks (`doWriteColValue`, `writeColumnWithRecordContext`) | misleading ERROR logs on a deadline cancel (review WR-05, open) | Warning (advisory) | Operator may chase a poison record for a transport timeout; thrown exception is correct. Listed under `advisory`, user disposition is backlog |

No stubs: the new code paths all execute real logic and are exercised end to end by the pins against a raw-socket stub.

### Human Verification Required

None. Every behavior-dependent truth (cancellation, re-arm on retry, release of the blocked writer, operator gate both ways, retriable-under-both-tolerances) has a passing named test that I confirmed red on the unchanged tree. The live-server numbers are recorded measurements the success criteria call for ("record"), not behaviors left to be exercised.

### Gaps Summary

No gaps. All four ROADMAP success criteria and TMO-01..03 hold in the code and tests. Open review items WR-05, IN-05 and IN-06 are advisory and carried by the disposition file; they do not touch a must-have.

---

_Verified: 2026-11-05T13:30:00Z_
_Verifier: Claude (gsd-verifier)_
