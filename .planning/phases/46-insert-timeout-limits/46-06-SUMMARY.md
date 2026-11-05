---
phase: 46-insert-timeout-limits
plan: "06"
subsystem: insert-timeout
tags: [gap-closure, review-fix, v2-write-deadline, client-retry, help-text, fork-delta, anchors, evidence]
status: complete

requires:
  - phase: 46-insert-timeout-limits
    provides: plans 46-01..46-05 and 46-REVIEW.md (WR-01..WR-05, IN-01..IN-07); review tip 62ab21c9, testUnit 2170, full suite 2773, anchors rows=214
provides:
  - the V2 write deadline re-armed on a client-internal retry (deadlineAwareWriter.onRetry -> InsertWriteDeadline.rearm)
  - clientInternalRetryOfAnInsertGetsAFreshWriteDeadline (red on the unchanged tree, green after, mutation-checked)
  - StubClickHouseServer.closingFirstPostAfterItsBodyThenNotReadingBody and its premise pin
  - the corrected help text of clickhouseClientInsertTimeoutMs and 9 changed or new pin phrases plus 4 retired-claim pins
  - FORK-DELTA item 14, the per-insert socket_timeout row and the write-deadline row corrected
  - evidence E-15..E-19; the E-01..E-19 index complete
affects: [49]

actuals:
  tokens: 13020
  tasks: 5
  commits: 6
plan_head_before: 62ab21c94b5bb7404d1406d16b9d2a39256f57b2
plan_head_after: 9b1f915718bc795fc5ea34a8d7dbe3b5d5a170e1

tech-stack:
  added: []
  patterns: [one shared DataStreamWriter wrapper per insert body so disarm and re-arm live in one place, a raw stub that drives a client retry, one pin phrase list tied to evidence, retired-claim pins]

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/ClickHouseWriter.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ClickHouseSinkConfig.java
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServer.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/helper/StubClickHouseServerRawModeUnitTest.java
    - .planning/scripts/fork-anchors.txt
    - docs/FORK-DELTA.md
    - .planning/phases/46-insert-timeout-limits/46-COMMAND-EVIDENCE.md

key-decisions:
  - "WR-01 is fixed by re-arming in DataStreamWriter.onRetry (the client's own hook, called before each further attempt), not by setting the client's retry option to 0: the retry stays available to connection-level faults and every attempt gets the full bound"
  - "IN-03 is fixed by renaming, not by strengthening: a fire against a finished request cannot be produced from outside the writer, and an in-flight same-query-id second insert cannot be held past the bound because the per-insert socket_timeout equals the bound (a draft was red at 301 ms for that reason and was dropped)"
  - "The V1 help-text clause claims the response wait only; a write blocked on the 4 MiB pipe is named as not bounded; the 30 s per-attempt figure and retryCount attempts are from the code, not measured with 3"

requirements-completed: [TMO-01, TMO-03]

coverage:
  - id: D1
    description: "A client V2 insert that the client retries internally gets the full write deadline on the retried attempt: the stub closes the first POST after its body and stops reading the second, and the insert ends with the bound's RetriableException after two POSTs."
    requirement: TMO-03
    verification:
      - kind: other
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertWriteStallBoundPinUnitTest.java#clientInternalRetryOfAnInsertGetsAFreshWriteDeadline (E-15 red on the unchanged tree at the 10 s guard, E-16 green at 1.088 s, mutation d5 red)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The help text of clickhouseClientInsertTimeoutMs no longer claims more than E-07..E-11 measured on client V1, the retry count, the ping setting and socket_timeout, and the pin carries the corrected wording."
    requirement: TMO-01
    verification:
      - kind: other
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/InsertTimeoutHelpTextPinUnitTest.java (E-17: 13 of 27 red on the previous text, 27 of 27 green after)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Gap-closure gate: testUnit 2181 tests with Docker unreachable, full suite 2784 tests on ClickHouse 26.10.3.7 x client V2 with the 18 named skips, anchors rows=216, unedited standing tail gate PASS, nothing pushed."
    requirement: TMO-03
    verification:
      - kind: other
        ref: "46-COMMAND-EVIDENCE.md E-19 (re-checked against xml-g06-testunit and xml-g06-suite-1)"
        status: pass
    human_judgment: false
---

# Phase 46 Plan 46-06: Review gap closure Summary

Gap closure for `46-REVIEW.md`: the client V2 insert write deadline is re-armed when the client retries an insert, and the help text and FORK-DELTA wording that overclaimed are corrected, each with a pin that was shown red before the change.

## Finding disposition

| Finding | Status | Where |
|---------|--------|-------|
| WR-01 V2 write deadline not re-armed on a client-internal retry | fixed | `10fdbf41` (code, pin, anchors), `c12c095a` (Phase 41 row narrowed, deadline row); E-15 red, E-16 green and mutation |
| WR-02 V1 quiet insert still ends at the library's 30 s limit | fixed | `19c36eaa` (help text H17), `7ee5c02a` (item 14); E-17 |
| WR-03 V1 help text read as "a stalled body write is bounded" | fixed | `19c36eaa` (H3, H15, H16), `7ee5c02a`; E-17 |
| WR-04 "30 s" is per attempt, "the timeout setting" is `timeoutSeconds` | fixed | `19c36eaa` (H8), `7ee5c02a`; E-17 |
| WR-05 misleading ERROR logs on the RowBinary route after a deadline cancel | left open | not touched, by instruction |
| IN-01 lead sentence states a general bound | fixed | `19c36eaa` (H0); E-17 |
| IN-02 progress-header claim rests on a stub pin only | left open | not touched, by instruction |
| IN-03 `lateFireAfterASuccessfulV2InsertIsHarmless` cannot fail for its name | fixed | `10fdbf41`: renamed `secondInsertAfterTheFirstInsertsBoundHasElapsedSucceeds`, Javadoc says what it does not pin; E-16 d1 |
| IN-04 `socket_timeout` text vs `awaitInsertResponse` with `async=true` | fixed | `19c36eaa` (H9), `7ee5c02a`; E-17 |
| IN-05 V1 timeout ignores the result of `cancel(true)` | left open | not touched, by instruction |
| IN-06 `stop()` during an insert can surface `RejectedExecutionException` | left open | not touched, by instruction |
| IN-07 literal `E-NN` placeholder, schema-refresh retry bound | fixed | `7ee5c02a`; E-18 |

## What changed

- **Code (WR-01):** `deadlineAwareWriter(deadline, body)` replaces the three per-site lambdas in `doInsertRawBinaryV2`, `doInsertJsonV2` and `doInsertStringV2`. Its `onOutput` runs the body, closes the stream and disarms; its `onRetry` calls the new `InsertWriteDeadline.rearm()` (disarm, then arm). The decompiled client-v2 0.11.0 confirms the contract: `DataStreamWriter.onRetry()` is a default method called by `Client.insert` after a retryable failure and before the next attempt's `onOutput`; the default retry count is 3 and the retried faults are `NoHttpResponse`, `ConnectTimeout`, `ConnectionRequestTimeout` and `ServerRetryable`; the connector sets none of these.
- **Pin (WR-01):** `clientInternalRetryOfAnInsertGetsAFreshWriteDeadline` on a new raw stub that reads the whole first POST and closes with no response, then reads only the head of later requests. Red on the unchanged tree by the 10 s guard (10.031 s), green at 1.088 s with two POSTs. Mutation d5 (empty `onRetry`) turns exactly that case red.
- **Anchors:** `fork-anchors.txt` `deadline.disarm();` 4 -> 2, new rows `deadline.rearm();` 1 and `deadlineAwareWriter(` 4; mirror rows 214 -> 216.
- **Help text:** the lead is a limit for the phases listed; the V1 clause is the response wait and nothing else, names the pipe write as not bounded, and says a value above 30000 ms does not extend a quiet insert; the V1 non-insert 30 s figure is per attempt with up to `retryCount` attempts and the ping uses `timeoutSeconds`; `socket_timeout` removes the insert limits except the V2 `async=true` response wait.
- **Pin:** 9 required phrases changed or added (H0, H3, H8, H9, H10, H14, H15, H16, H17) and four retired-claim cases, 27 cases in all; 13 were red on the previous text.
- **FORK-DELTA:** the Phase 41 sentence "the client makes one request (no client-side retry)" now says a read timeout is not retried and names the faults that are; the write-deadline row records the re-arm; item 14 has real evidence ids instead of `E-NN`, the V1 30 s clause, the `async=true` exception, the schema-refresh bound (a fresh deadline on V2, a fresh response wait on V1, read from the code) and the corrected help-text scope description.

## Gate (E-19)

- `testUnit` with Docker unreachable: 2181 tests, 0 failures, 0 errors, 2 skipped, 103 files (2170 + 1 raw-stub case + 1 retry pin + 9 help-text cases).
- Full suite on ClickHouse 26.10.3.7 (digest unchanged since E-01) x client V2: 2784 tests, 0 failures, 0 errors, 18 skipped (the named inventory), 152 files, 3 min 14 s; no JVM crash.
- Standing tail gate (unedited script, `M..7ee5c02a`): `result=PASS`, `anchor_rows=216`, `anchor_commits=83`.
- Scope: `src/main` changed only in `ClickHouseWriter.java` (deadlineAwareWriter, `rearm`) and `ClickHouseSinkConfig.java` (help text only); no file added; gg7 pins byte-unchanged; build, workflow, CHANGELOG, VERSION and the gate scripts unchanged; `async_insert` lines in `src/main` still 2.
- Nothing pushed; no dispatch-isolation call; STATE.md, `.planning/config.json` and `.planning/state.json` not staged.

## Deviations from Plan

There was no PLAN.md; the dispatch prompt was the plan.

**1. [Rule 1 - test design] A first IN-03 replacement was wrong and was dropped.** Found during: the red run for WR-01. Issue: a draft that reused the query id and delayed the second response 900 ms was red on the unchanged tree at 301 ms because the per-insert `socket_timeout` equals the 300 ms bound and ends a quiet in-flight insert. Fix: the draft was never committed; the case is renamed to what it pins (the dispatch allowed this). The red evidence was repeated with the final class (E-15). Files modified: `InsertWriteStallBoundPinUnitTest.java`. Commit: `10fdbf41`.

**Total deviations:** 1 (a discarded test draft). **Impact:** none on the delivered code; the IN-03 outcome is a rename, not a strengthened case.

## Known Stubs

None.

## Threat Flags

None. No network endpoint, auth path or schema surface was added; the watchdog and its cancel call are unchanged, `rearm` only reschedules the same task.

## Residual notes for the reviewer

- The `retryCount` attempts clause (H8) and the `async=true` exception (H9) are read from the code, not measured; E-17 says so.
- On the final red run the retry pin ends in the 10 s guard, not in a recorded POST count; that a second attempt was sent follows from the unchanged tree not returning the closed-connection failure at once, and the green run asserts two POSTs.
- A retry after a first attempt that failed before its body closed is covered by `rearm()` cancelling the stale task first; no pin drives that exact shape.

## Self-Check: PASSED

- Created and modified files exist on disk (the nine files in `key-files.modified`).
- Commits `9cedea94`, `10fdbf41`, `c12c095a`, `19c36eaa`, `7ee5c02a` and `9b1f9157` are ancestors of HEAD; `git rev-list --count 62ab21c9..HEAD` read 6 before this file was committed.
- The evidence index reads E-01..E-19 all `pass`; E-15..E-19 re-read with `46-evidence-check.py expect`.
